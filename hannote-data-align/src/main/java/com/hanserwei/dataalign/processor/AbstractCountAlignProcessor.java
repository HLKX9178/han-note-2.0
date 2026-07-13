package com.hanserwei.dataalign.processor;

import cn.hutool.core.collection.CollUtil;
import com.hanserwei.dataalign.constant.TableConstants;
import com.hanserwei.dataalign.enums.EsSyncDimensionEnum;
import com.hanserwei.dataalign.model.task.AlignShardTask;
import com.hanserwei.dataalign.processor.support.CountAlignSupport;
import org.springframework.data.redis.core.RedisTemplate;
import tech.powerjob.worker.core.processor.ProcessResult;
import tech.powerjob.worker.core.processor.TaskContext;
import tech.powerjob.worker.core.processor.TaskResult;
import tech.powerjob.worker.core.processor.sdk.MapReduceProcessor;
import tech.powerjob.worker.log.OmsLogger;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * 分片计数对齐任务抽象基类（PowerJob MapReduce）.
 *
 * <p>统一 MapReduce 骨架：
 * <ul>
 *   <li><b>根任务</b>：按分片总数生成 N 个 {@link AlignShardTask} 子任务，{@code map()} 分发到各 worker；</li>
 *   <li><b>子任务</b>：对指定分片的<strong>昨日</strong>日增量表分批处理——
 *       查一批变更 ID → 源头 {@code count(*)} 真实值 → 回写计数表 → 同步 Redis 缓存（仅当存在）→ 批量物删，
 *       直到该分片表清空。</li>
 * </ul>
 *
 * <p>子类只需提供「查哪张表、count 哪张源表、写哪个计数字段、写哪个缓存 Key/Field、删哪张表」，
 * 各自成为独立的 Spring Bean（PowerJob 按全限定类名解析），互不干扰。
 *
 * @author hanserwei
 * @date 2026/07/11
 * @since 0.0.1
 */
public abstract class AbstractCountAlignProcessor implements MapReduceProcessor {

    protected static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd");

    /** 单批处理条数 */
    private static final int BATCH_SIZE = 1000;

    /** 子任务名前缀（用于 map 分发标识） */
    private static final String SUBTASK_NAME = "ALIGN_SHARDS";

    protected final CountAlignSupport support;

    protected AbstractCountAlignProcessor(CountAlignSupport support) {
        this.support = support;
    }

    @Override
    public ProcessResult process(TaskContext context) throws Exception {
        OmsLogger omsLogger = context.getOmsLogger();

        // 根任务：按分片数切分子任务
        if (isRootTask()) {
            int shards = support.getTableShardProperties().getShards();
            List<AlignShardTask> subTasks = new ArrayList<>(shards);
            for (int i = 0; i < shards; i++) {
                subTasks.add(new AlignShardTask(i));
            }
            map(subTasks, SUBTASK_NAME);
            omsLogger.info("=================> [{}] 根任务分发 {} 个分片子任务", taskName(), shards);
            return new ProcessResult(true, taskName() + " mapped " + shards + " shards");
        }

        // 子任务：对齐单个分片
        AlignShardTask subTask = (AlignShardTask) context.getSubTask();
        int shardIndex = subTask.getShardIndex();
        int processed = alignShard(shardIndex, omsLogger);
        return new ProcessResult(true, taskName() + " shard " + shardIndex + " aligned " + processed);
    }

    @Override
    public ProcessResult reduce(TaskContext context, List<TaskResult> taskResults) {
        context.getOmsLogger().info("=================> [{}] 全部分片对齐完成", taskName());
        return new ProcessResult(true, taskName() + " reduce done");
    }

    /**
     * 对齐单个分片：分批查询昨日日增量表 → count 真实值 → 回写计数表与缓存 → 批量物删。
     *
     * @param shardIndex 分片序号
     * @param omsLogger  在线日志
     * @return 本分片共对齐的记录数
     */
    private int alignShard(int shardIndex, OmsLogger omsLogger) {
        // 对齐昨日发生变更的数据
        String date = LocalDate.now().minusDays(1).format(DATE_FORMATTER);
        String tableNameSuffix = TableConstants.buildTableNameSuffix(date, shardIndex);
        omsLogger.info("=================> [{}] 开始对齐, 分片={}, 表后缀={}", taskName(), shardIndex, tableNameSuffix);

        RedisTemplate<String, Object> redisTemplate = support.getRedisTemplate();
        int processedTotal = 0;

        for (; ; ) {
            List<Long> ids = selectBatch(tableNameSuffix, BATCH_SIZE);
            if (CollUtil.isEmpty(ids)) {
                break;
            }

            for (Long id : ids) {
                // 源头表 count 真实值
                int total = countReal(id);
                // 回写计数表
                if (updateCount(id, total) > 0) {
                    // 同步 Redis 缓存（仅当 Hash key 已存在，不主动创建）
                    String cacheKey = buildCacheKey(id);
                    if (Boolean.TRUE.equals(redisTemplate.hasKey(cacheKey))) {
                        redisTemplate.opsForHash().put(cacheKey, cacheField(), total);
                    }
                    // 通知搜索服务刷新 ES 索引中冗余的计数（仅笔记/用户维度，尽力而为）
                    notifySearchEsSync(id);
                }
            }

            // 批量物理删除已对齐的这批记录
            batchDelete(tableNameSuffix, ids);
            processedTotal += ids.size();
        }

        omsLogger.info("=================> [{}] 结束对齐, 分片={}, 共对齐 {} 条", taskName(), shardIndex, processedTotal);
        return processedTotal;
    }

    /**
     * 计数对齐成功后，按维度通知搜索服务重建对应文档以刷新 ES 冗余计数。
     */
    private void notifySearchEsSync(long id) {
        switch (esSyncDimension()) {
            case NOTE -> support.getSearchEsSyncSender().syncNote(id);
            case USER -> support.getSearchEsSyncSender().syncUser(id);
            case NONE -> { /* 该计数不在 ES 索引中，无需同步 */ }
        }
    }

    // ------------------------- 子类提供的每维度差异 -------------------------

    /** 任务名（日志标识） */
    protected abstract String taskName();

    /**
     * 本对齐任务的计数对应哪个 ES 索引维度（决定是否/如何通知搜索服务）。
     * 默认 {@link EsSyncDimensionEnum#NONE}（不涉及 ES）。
     */
    protected EsSyncDimensionEnum esSyncDimension() {
        return EsSyncDimensionEnum.NONE;
    }

    /** 分批查询日增量表，返回变更的 ID 列表 */
    protected abstract List<Long> selectBatch(String tableNameSuffix, int batchSize);

    /** 源头表 count(*) 真实值 */
    protected abstract int countReal(long id);

    /** 回写计数表，返回受影响行数 */
    protected abstract int updateCount(long id, int total);

    /** 构建计数缓存 Redis Key（用户维度 or 笔记维度） */
    protected abstract String buildCacheKey(long id);

    /** 计数缓存 Hash Field */
    protected abstract String cacheField();

    /** 批量物理删除日增量表中已对齐的记录 */
    protected abstract void batchDelete(String tableNameSuffix, List<Long> ids);
}
