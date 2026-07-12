package com.hanserwei.dataalign.consumer;

import com.hanserwei.dataalign.config.TableShardProperties;
import com.hanserwei.dataalign.constant.MQConstants;
import com.hanserwei.dataalign.constant.RedisKeyConstants;
import com.hanserwei.dataalign.constant.TableConstants;
import com.hanserwei.dataalign.domain.mapper.InsertMapper;
import com.hanserwei.dataalign.model.dto.CollectUnCollectNoteMqDTO;
import com.hanserwei.dataalign.util.BloomFilterExecutor;
import com.hanserwei.framework.common.util.JsonUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Objects;

/**
 * 日增量落库：笔记收藏 / 取消收藏.
 *
 * <p>消费 {@code CountNoteCollectTopic}，记录两个变更维度：笔记被收藏数（noteId）与
 * 发布者获藏数（noteCreatorId）。同样应用 10418 双布隆修复（noteId、noteCreatorId 各自独立）。
 *
 * @author hanserwei
 * @date 2026/07/11
 * @since 0.0.1
 */
@Slf4j
@Component
@RequiredArgsConstructor
@RocketMQMessageListener(
        consumerGroup = MQConstants.GROUP_DATA_ALIGN_NOTE_COLLECT,
        topic = MQConstants.TOPIC_COUNT_NOTE_COLLECT)
public class TodayNoteCollectIncrementData2DBConsumer implements RocketMQListener<String> {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd");

    private final InsertMapper insertMapper;
    private final BloomFilterExecutor bloomFilterExecutor;
    private final TableShardProperties tableShardProperties;

    @Override
    public void onMessage(String body) {
        log.info("## TodayNoteCollectIncrementData2DBConsumer 消费到 MQ: {}", body);

        CollectUnCollectNoteMqDTO dto = JsonUtils.parseObject(body, CollectUnCollectNoteMqDTO.class);
        if (Objects.isNull(dto)) {
            return;
        }

        Long noteId = dto.getNoteId();
        Long noteCreatorId = dto.getNoteCreatorId();
        String date = LocalDate.now().format(DATE_FORMATTER);
        int shards = tableShardProperties.getShards();

        // ---------- 笔记维度：被收藏数变更 ----------
        if (Objects.nonNull(noteId)) {
            String noteBloomKey = RedisKeyConstants.buildBloomNoteCollectNoteIdKey(date);
            if (bloomFilterExecutor.isAbsent(noteBloomKey, noteId)) {
                try {
                    insertMapper.insertNoteCollectCountTemp(
                            TableConstants.buildTableNameSuffix(date, noteId % shards), noteId);
                    bloomFilterExecutor.add(noteBloomKey, noteId);
                } catch (Exception e) {
                    log.error("## 落库笔记收藏数日增量失败, noteId={}", noteId, e);
                }
            }
        }

        // ---------- 用户维度：发布者获藏数变更 ----------
        if (Objects.nonNull(noteCreatorId)) {
            String userBloomKey = RedisKeyConstants.buildBloomNoteCollectUserIdKey(date);
            if (bloomFilterExecutor.isAbsent(userBloomKey, noteCreatorId)) {
                try {
                    insertMapper.insertUserCollectCountTemp(
                            TableConstants.buildTableNameSuffix(date, noteCreatorId % shards), noteCreatorId);
                    bloomFilterExecutor.add(userBloomKey, noteCreatorId);
                } catch (Exception e) {
                    log.error("## 落库用户获藏数日增量失败, noteCreatorId={}", noteCreatorId, e);
                }
            }
        }
    }
}
