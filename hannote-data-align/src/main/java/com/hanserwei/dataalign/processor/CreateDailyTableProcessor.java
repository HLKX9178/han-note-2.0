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
 * <p>每日 23:00 触发，为每个分片创建 7 张明日临时表（{@code CREATE TABLE IF NOT EXISTS}），
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

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd");

    private final CreateTableMapper createTableMapper;
    private final TableShardProperties tableShardProperties;

    @Override
    public ProcessResult process(TaskContext context) {
        OmsLogger omsLogger = context.getOmsLogger();

        int shards = tableShardProperties.getShards();
        String date = LocalDate.now().plusDays(1).format(DATE_FORMATTER);
        omsLogger.info("## 开始创建明日日增量表, date={}, shards={}", date, shards);
        log.info("## 开始创建明日日增量表, date={}, shards={}", date, shards);

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
                omsLogger.info("## 已创建分片 {} 的 7 张日增量表, suffix={}", hashKey, suffix);
            } catch (Exception e) {
                omsLogger.error("## 创建分片 {} 日增量表失败, suffix={}", hashKey, suffix, e);
                log.error("## 创建分片 {} 日增量表失败, suffix={}", hashKey, suffix, e);
            }
        }

        omsLogger.info("## 结束创建明日日增量表, date={}", date);
        return new ProcessResult(true, "created daily tables for " + date + ", shards=" + shards);
    }
}
