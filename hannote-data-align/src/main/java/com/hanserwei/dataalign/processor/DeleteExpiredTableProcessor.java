package com.hanserwei.dataalign.processor;

import com.hanserwei.dataalign.config.TableShardProperties;
import com.hanserwei.dataalign.constant.TableConstants;
import com.hanserwei.dataalign.domain.mapper.DeleteTableMapper;
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
 * PowerJob 单机任务：删除近一个月已对齐完成的日增量临时表.
 *
 * <p>每日 23:30 触发。分片对齐任务在凌晨 3 点已把昨日增量对齐完毕，故从「昨天」开始往前回溯
 * 一个月（不含今天，今天的表仍在收集当日增量、待明日凌晨对齐），逐张 {@code DROP TABLE IF EXISTS}。
 *
 * <p>控制台配置：处理器类型=内置Java，执行类型=单机，CRON={@code 0 30 23 * * ?}。
 *
 * @author hanserwei
 * @date 2026/07/11
 * @since 0.0.1
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class DeleteExpiredTableProcessor implements BasicProcessor {

    /** 表名后缀的日期格式（yyyyMMdd） */
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd");

    private final DeleteTableMapper deleteTableMapper;
    private final TableShardProperties tableShardProperties;

    /**
     * 清理近一个月已对齐完成的日增量临时表.
     *
     * <p>单机任务：从「昨天」向前回溯一个月逐日、逐分片 DROP，跳过今天（今日表仍在收集当日增量、
     * 待明日凌晨对齐）。单张删表失败只记日志、不中断，整体返回成功。
     *
     * @param context PowerJob 任务上下文，用于获取控制台在线日志 {@link OmsLogger}
     * @return 删表结果（携带分片数）
     */
    @Override
    public ProcessResult process(TaskContext context) {
        OmsLogger omsLogger = context.getOmsLogger();

        int shards = tableShardProperties.getShards();
        omsLogger.info("## 开始删除近一个月已对齐的日增量临时表, shards={}", shards);
        log.info("## 开始删除近一个月已对齐的日增量临时表, shards={}", shards);

        // 1. 回溯区间：(今天-1个月, 今天)，不含今天
        LocalDate today = LocalDate.now();
        LocalDate endDate = today.minusMonths(1);
        LocalDate cursor = today;

        // 2. 逐日向前推进，每日逐分片 DROP 8 张临时表
        while (cursor.isAfter(endDate)) {
            // 从昨天开始往前推，不含今天
            cursor = cursor.minusDays(1);
            String date = cursor.format(DATE_FORMATTER);

            for (int hashKey = 0; hashKey < shards; hashKey++) {
                String suffix = TableConstants.buildTableNameSuffix(date, hashKey);
                try {
                    deleteTableMapper.dropDataAlignFollowingCountTempTable(suffix);
                    deleteTableMapper.dropDataAlignFansCountTempTable(suffix);
                    deleteTableMapper.dropDataAlignNoteCollectCountTempTable(suffix);
                    deleteTableMapper.dropDataAlignUserCollectCountTempTable(suffix);
                    deleteTableMapper.dropDataAlignUserLikeCountTempTable(suffix);
                    deleteTableMapper.dropDataAlignNoteLikeCountTempTable(suffix);
                    deleteTableMapper.dropDataAlignNotePublishCountTempTable(suffix);
                    deleteTableMapper.dropDataAlignNoteCommentCountTempTable(suffix);
                } catch (Exception e) {
                    omsLogger.error("## 删除日增量表失败, suffix={}", suffix, e);
                    log.error("## 删除日增量表失败, suffix={}", suffix, e);
                }
            }
        }

        omsLogger.info("## 结束删除近一个月已对齐的日增量临时表");
        return new ProcessResult(true, "dropped last-month temp tables, shards=" + shards);
    }
}
