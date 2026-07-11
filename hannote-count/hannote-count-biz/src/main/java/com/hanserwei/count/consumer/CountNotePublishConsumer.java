package com.hanserwei.count.consumer;

import com.hanserwei.count.constant.MQConstants;
import com.hanserwei.count.constant.RedisKeyConstants;
import com.hanserwei.count.domain.mapper.UserCountDOMapper;
import com.hanserwei.count.model.dto.NoteOperateMqDTO;
import com.hanserwei.framework.common.util.JsonUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.common.message.MessageExt;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.Objects;

/**
 * 计数：用户发布笔记数消费者（低并发，直写）.
 *
 * <p>发布/删除笔记并发远低于点赞/收藏，无需聚合。按 Tag 分派：发布 +1、删除 -1，
 * 直接 {@code HINCRBY} 用户维度发布笔记数（仅当 key 存在）并 upsert {@code t_user_count.note_total}。
 *
 * @author hanserwei
 * @date 2026/07/11
 * @since 0.0.1
 */
@Slf4j
@Component
@RequiredArgsConstructor
@RocketMQMessageListener(
        consumerGroup = MQConstants.GROUP_COUNT_NOTE_OPERATE,
        topic = MQConstants.TOPIC_NOTE_OPERATE)
public class CountNotePublishConsumer implements RocketMQListener<MessageExt> {

    private final RedisTemplate<String, Object> redisTemplate;
    private final UserCountDOMapper userCountDOMapper;

    @Override
    public void onMessage(MessageExt message) {
        String body = new String(message.getBody(), StandardCharsets.UTF_8);
        String tags = message.getTags();
        log.info("==> CountNotePublishConsumer 消费消息: {}, tags: {}", body, tags);

        if (Objects.equals(tags, MQConstants.TAG_NOTE_PUBLISH)) {
            handleTagMessage(body, 1);
        } else if (Objects.equals(tags, MQConstants.TAG_NOTE_DELETE)) {
            handleTagMessage(body, -1);
        }
    }

    /**
     * 处理笔记发布 / 删除计数。
     *
     * @param body  消息体 JSON
     * @param count 增量：发布 +1，删除 -1
     */
    private void handleTagMessage(String body, int count) {
        NoteOperateMqDTO dto = JsonUtils.parseObject(body, NoteOperateMqDTO.class);
        if (Objects.isNull(dto) || Objects.isNull(dto.getCreatorId())) {
            return;
        }

        Long creatorId = dto.getCreatorId();

        // 更新 Redis 用户维度发布笔记数（仅当 key 存在）
        String userKey = RedisKeyConstants.buildCountUserKey(creatorId);
        if (Boolean.TRUE.equals(redisTemplate.hasKey(userKey))) {
            redisTemplate.opsForHash().increment(userKey, RedisKeyConstants.FIELD_NOTE_TOTAL, count);
        }

        // 落库
        userCountDOMapper.insertOrUpdateNoteTotalByUserId(count, creatorId);
    }
}
