package com.hanserwei.note.consumer;

import com.hanserwei.framework.common.util.JsonUtils;
import com.hanserwei.note.constant.MQConstants;
import com.hanserwei.note.constant.RedisKeyConstants;
import com.hanserwei.note.model.dto.DelayDeleteNoteCacheMqDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * 延迟双删 Redis 笔记缓存消费者（集群模式）.
 *
 * <p>笔记更新 / 删除 / 发布接口在「先删缓存 → 写库」后，异步发送延时消息，约 1s 后由本消费者
 * 二次删除「笔记详情缓存」与「作者已发布笔记列表缓存」，尽量避免并发场景下查询接口把旧数据
 * 回填到缓存导致的脏读。
 *
 * @author hanserwei
 * @date 2026/07/09
 * @since 0.0.1
 */
@Slf4j
@Component
@RequiredArgsConstructor
@RocketMQMessageListener(
        consumerGroup = MQConstants.GROUP_DELAY_DELETE_NOTE_REDIS_CACHE,
        topic = MQConstants.TOPIC_DELAY_DELETE_NOTE_REDIS_CACHE)
public class DelayDeleteNoteRedisCacheConsumer implements RocketMQListener<String> {

    private final RedisTemplate<String, Object> redisTemplate;

    @Override
    public void onMessage(String body) {
        DelayDeleteNoteCacheMqDTO dto = JsonUtils.parseObject(body, DelayDeleteNoteCacheMqDTO.class);
        if (Objects.isNull(dto)) {
            return;
        }
        Long noteId = dto.getNoteId();
        Long userId = dto.getUserId();
        log.info("## 延时消费：二次删除 Redis 笔记缓存, noteId: {}, userId: {}", noteId, userId);

        List<String> keys = new ArrayList<>(2);
        if (Objects.nonNull(noteId)) {
            keys.add(RedisKeyConstants.buildNoteDetailKey(noteId));
        }
        if (Objects.nonNull(userId)) {
            keys.add(RedisKeyConstants.buildPublishedNoteListKey(userId));
        }
        if (!keys.isEmpty()) {
            redisTemplate.delete(keys);
        }
    }
}
