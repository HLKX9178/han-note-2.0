package com.hanserwei.dataalign.processor;

import com.hanserwei.dataalign.config.TableShardProperties;
import com.hanserwei.dataalign.constant.TableConstants;
import com.hanserwei.dataalign.domain.mapper.CreateTableMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import tech.powerjob.worker.core.processor.ProcessResult;
import tech.powerjob.worker.core.processor.TaskContext;
import tech.powerjob.worker.core.processor.sdk.BasicProcessor;
import tech.powerjob.worker.log.OmsLogger;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * PowerJob 单机任务：提前创建明日的日增量临时表.
 *
 * <p>每日 23:00 触发，为每个分片创建 8 张明日临时表（{@code CREATE TABLE IF NOT EXISTS}），
 * 承接次日发生变更、需重新对齐的 userId/noteId。
 *
 * <p>控制台配置：处理器类型=内置Java，执行类型=单机，CRON={@code 0 0 23 * * ?}。
 *
 * @author hanserwei
 * @date 2026/07/11
 * @since 0.0.1
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class CreateDailyTableProcessor implements BasicProcessor {

    /** 表名后缀的日期格式（yyyyMMdd） */
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd");

    private final CreateTableMapper createTableMapper;
    private final TableShardProperties tableShardProperties;

    /**
     * 提前建好明日全部分片的 8 张日增量临时表.
     *
     * <p>单机任务：任一分片建表失败只记日志、不中断其余分片，最终整体返回成功，
     * 避免个别 DDL 异常导致次日增量无表可落。
     *
     * @param context PowerJob 任务上下文，用于获取控制台在线日志 {@link OmsLogger}
     * @return 建表结果（携带日期与分片数）
     */
    @Override
    public ProcessResult process(TaskContext context) {
        OmsLogger omsLogger = context.getOmsLogger();

        // 1. 取分片数，并把日期定位到「明天」（提前一天建表）
        int shards = tableShardProperties.getShards();
        String date = LocalDate.now().plusDays(1).format(DATE_FORMATTER);
        omsLogger.info("## 开始创建明日日增量表, date={}, shards={}", date, shards);
        log.info("## 开始创建明日日增量表, date={}, shards={}", date, shards);

        // 2. 逐分片创建 8 张临时表，单分片失败不影响其余分片
        for (int hashKey = 0; hashKey < shards; hashKey++) {
            String suffix = TableConstants.buildTableNameSuffix(date, hashKey);
            try {
                createTableMapper.createDataAlignFollowingCountTempTable(suffix);
                createTableMapper.createDataAlignFansCountTempTable(suffix);
                createTableMapper.createDataAlignNoteCollectCountTempTable(suffix);
                createTableMapper.createDataAlignUserCollectCountTempTable(suffix);
                createTableMapper.createDataAlignUserLikeCountTempTable(suffix);
                createTableMapper.createDataAlignNoteLikeCountTempTable(suffix);
                createTableMapper.createDataAlignNotePublishCountTempTable(suffix);
                createTableMapper.createDataAlignNoteCommentCountTempTable(suffix);
                omsLogger.info("## 已创建分片 {} 的 8 张日增量表, suffix={}", hashKey, suffix);
            } catch (Exception e) {
                omsLogger.error("## 创建分片 {} 日增量表失败, suffix={}", hashKey, suffix, e);
                log.error("## 创建分片 {} 日增量表失败, suffix={}", hashKey, suffix, e);
            }
        }

        omsLogger.info("## 结束创建明日日增量表, date={}", date);
        return new ProcessResult(true, "created daily tables for " + date + ", shards=" + shards);
    }
}
