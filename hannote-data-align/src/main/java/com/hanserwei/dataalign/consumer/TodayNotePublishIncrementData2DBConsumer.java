package com.hanserwei.dataalign.consumer;

import com.hanserwei.dataalign.config.TableShardProperties;
import com.hanserwei.dataalign.constant.MQConstants;
import com.hanserwei.dataalign.constant.RedisKeyConstants;
import com.hanserwei.dataalign.constant.TableConstants;
import com.hanserwei.dataalign.domain.mapper.InsertMapper;
import com.hanserwei.dataalign.model.dto.NoteOperateMqDTO;
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
 * 日增量落库：笔记发布 / 删除.
 *
 * <p>消费 {@code NoteOperateTopic}，只对应一个变更维度：用户已发布笔记数（creatorId）。
 *
 * @author hanserwei
 * @date 2026/07/11
 * @since 0.0.1
 */
@Slf4j
@Component
@RequiredArgsConstructor
@RocketMQMessageListener(
        consumerGroup = MQConstants.GROUP_DATA_ALIGN_NOTE_OPERATE,
        topic = MQConstants.TOPIC_NOTE_OPERATE)
public class TodayNotePublishIncrementData2DBConsumer implements RocketMQListener<String> {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd");

    private final InsertMapper insertMapper;
    private final BloomFilterExecutor bloomFilterExecutor;
    private final TableShardProperties tableShardProperties;

    @Override
    public void onMessage(String body) {
        log.info("## TodayNotePublishIncrementData2DBConsumer 消费到 MQ: {}", body);

        NoteOperateMqDTO dto = JsonUtils.parseObject(body, NoteOperateMqDTO.class);
        if (Objects.isNull(dto)) {
            return;
        }

        Long creatorId = dto.getCreatorId();
        if (Objects.isNull(creatorId)) {
            return;
        }

        String date = LocalDate.now().format(DATE_FORMATTER);
        int shards = tableShardProperties.getShards();

        String bloomKey = RedisKeyConstants.buildBloomNoteOperateUserIdKey(date);
        if (bloomFilterExecutor.isAbsent(bloomKey, creatorId)) {
            try {
                insertMapper.insertNotePublishCountTemp(
                        TableConstants.buildTableNameSuffix(date, creatorId % shards), creatorId);
                bloomFilterExecutor.add(bloomKey, creatorId);
            } catch (Exception e) {
                log.error("## 落库用户发布笔记数日增量失败, creatorId={}", creatorId, e);
            }
        }
    }
}
