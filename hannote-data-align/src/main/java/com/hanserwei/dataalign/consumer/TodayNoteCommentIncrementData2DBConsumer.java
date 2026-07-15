package com.hanserwei.dataalign.consumer;

import com.hanserwei.dataalign.config.TableShardProperties;
import com.hanserwei.dataalign.constant.MQConstants;
import com.hanserwei.dataalign.constant.RedisKeyConstants;
import com.hanserwei.dataalign.constant.TableConstants;
import com.hanserwei.dataalign.domain.mapper.InsertMapper;
import com.hanserwei.dataalign.model.dto.CommentCountChangedMqDTO;
import com.hanserwei.dataalign.util.BloomFilterExecutor;
import com.hanserwei.framework.common.util.JsonUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * 日增量落库：笔记评论总数变更.
 *
 * @author hanserwei
 * @date 2026/07/15
 * @since 0.0.1
 */
@Slf4j
@Component
@RequiredArgsConstructor
@RocketMQMessageListener(
        consumerGroup = MQConstants.GROUP_DATA_ALIGN_NOTE_COMMENT,
        topic = MQConstants.TOPIC_COMMENT_COUNT_CHANGED)
public class TodayNoteCommentIncrementData2DBConsumer implements RocketMQListener<String> {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd");

    private final InsertMapper insertMapper;
    private final BloomFilterExecutor bloomFilterExecutor;
    private final TableShardProperties tableShardProperties;

    @Override
    public void onMessage(String body) {
        CommentCountChangedMqDTO dto = JsonUtils.parseObject(body, CommentCountChangedMqDTO.class);
        if (dto == null || dto.getNoteId() == null) {
            return;
        }
        Long noteId = dto.getNoteId();
        String date = LocalDate.now().format(DATE_FORMATTER);
        String bloomKey = RedisKeyConstants.buildBloomNoteCommentNoteIdKey(date);
        if (bloomFilterExecutor.isAbsent(bloomKey, noteId)) {
            insertMapper.insertNoteCommentCountTemp(
                    TableConstants.buildTableNameSuffix(date, noteId % tableShardProperties.getShards()), noteId);
            bloomFilterExecutor.add(bloomKey, noteId);
        }
    }
}
