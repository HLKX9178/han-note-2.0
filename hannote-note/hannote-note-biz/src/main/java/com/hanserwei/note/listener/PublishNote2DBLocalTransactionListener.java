package com.hanserwei.note.listener;

import com.hanserwei.framework.common.util.JsonUtils;
import com.hanserwei.note.constant.MQConstants;
import com.hanserwei.note.constant.RedisKeyConstants;
import com.hanserwei.note.convert.NoteConvert;
import com.hanserwei.note.domain.dataobject.NoteDO;
import com.hanserwei.note.domain.mapper.NoteDOMapper;
import com.hanserwei.note.enums.NoteOperateEnum;
import com.hanserwei.note.model.dto.DelayDeleteNoteCacheMqDTO;
import com.hanserwei.note.model.dto.NoteOperateMqDTO;
import com.hanserwei.note.model.dto.PublishNoteDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.client.producer.SendCallback;
import org.apache.rocketmq.client.producer.SendResult;
import org.apache.rocketmq.spring.annotation.RocketMQTransactionListener;
import org.apache.rocketmq.spring.core.RocketMQLocalTransactionListener;
import org.apache.rocketmq.spring.core.RocketMQLocalTransactionState;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;

/**
 * 发布笔记事务消息：本地事务监听器.
 *
 * <p>Broker 收到 half 消息后回调 {@link #executeLocalTransaction} 执行本地事务（写 t_note 元数据）：
 * 成功则 COMMIT，half 消息转正式消息、KV 消费者写正文；失败则 ROLLBACK，作废 half 消息、KV 不落库，
 * 从而保证 t_note 与 ScyllaDB 正文的最终一致。生产者未及时反馈时由 {@link #checkLocalTransaction} 回查兜底。
 *
 * @author hanserwei
 * @date 2026/07/18
 * @since 0.0.1
 */
@Slf4j
@Component
@RequiredArgsConstructor
@RocketMQTransactionListener
public class PublishNote2DBLocalTransactionListener implements RocketMQLocalTransactionListener {

    private final RedisTemplate<String, Object> redisTemplate;
    private final NoteDOMapper noteDOMapper;
    private final RocketMQTemplate rocketMQTemplate;

    @Override
    public RocketMQLocalTransactionState executeLocalTransaction(Message msg, Object arg) {
        PublishNoteDTO publishNoteDTO = parsePayload(msg);
        if (publishNoteDTO == null) {
            return RocketMQLocalTransactionState.ROLLBACK;
        }
        Long noteId = publishNoteDTO.getId();
        Long creatorId = publishNoteDTO.getCreatorId();
        log.info("==> 事务消息：开始执行本地事务, noteId: {}", noteId);

        // 1. 删除作者已发布列表缓存（延迟双删第一步）
        redisTemplate.delete(RedisKeyConstants.buildPublishedNoteListKey(creatorId));

        // 2. 执行本地事务：元数据入库，失败则 ROLLBACK 作废 half 消息（KV 不落库）
        try {
            NoteDO noteDO = NoteConvert.INSTANCE.convertDTO2DO(publishNoteDTO);
            noteDOMapper.insert(noteDO);
        } catch (Exception e) {
            log.error("==> 事务消息：笔记元数据入库失败, noteId: {}", noteId, e);
            return RocketMQLocalTransactionState.ROLLBACK;
        }

        // 3. 副作用（入库成功后发送，均尽力而为，失败不影响 COMMIT）
        // 3.1 延迟双删第二步：约 1s 后二次删列表缓存
        sendDelayDeleteNoteRedisCacheMq(noteId, creatorId);
        // 3.2 通知计数服务：发布数 +1
        sendNoteOperateMq(creatorId, noteId);
        // 3.3 通知搜索服务：重建 ES 文档
        sendNoteSyncEsMq(noteId);

        // 4. 提交事务：half 消息转正式消息，KV 消费者可见并写正文
        return RocketMQLocalTransactionState.COMMIT;
    }

    @Override
    public RocketMQLocalTransactionState checkLocalTransaction(Message msg) {
        PublishNoteDTO publishNoteDTO = parsePayload(msg);
        if (publishNoteDTO == null) {
            return RocketMQLocalTransactionState.ROLLBACK;
        }
        Long noteId = publishNoteDTO.getId();
        log.info("==> 事务消息：开始事务回查, noteId: {}", noteId);

        // 元数据已入库说明本地事务执行成功，否则失败
        int count = noteDOMapper.selectCountByNoteId(noteId);
        return count >= 1 ? RocketMQLocalTransactionState.COMMIT : RocketMQLocalTransactionState.ROLLBACK;
    }

    /**
     * 解析消息体为 {@link PublishNoteDTO}（兼容 byte[] / String 载荷）.
     */
    private PublishNoteDTO parsePayload(Message msg) {
        try {
            Object payload = msg.getPayload();
            String json = payload instanceof byte[] bytes
                    ? new String(bytes, StandardCharsets.UTF_8) : String.valueOf(payload);
            return JsonUtils.parseObject(json, PublishNoteDTO.class);
        } catch (Exception e) {
            log.error("==> 事务消息：消息体解析失败", e);
            return null;
        }
    }

    /**
     * 延迟双删第二步：异步发延时消息，约 1s 后二次删列表缓存（详情缓存此时尚未生成，一并删无副作用）.
     */
    private void sendDelayDeleteNoteRedisCacheMq(Long noteId, Long userId) {
        try {
            DelayDeleteNoteCacheMqDTO dto = DelayDeleteNoteCacheMqDTO.builder()
                    .noteId(noteId).userId(userId).build();
            rocketMQTemplate.syncSendDelayTimeSeconds(MQConstants.TOPIC_DELAY_DELETE_NOTE_REDIS_CACHE,
                    JsonUtils.toJsonString(dto), 1);
            log.info("==> MQ：延时删除 Redis 笔记缓存消息发送成功, noteId: {}, userId: {}", noteId, userId);
        } catch (Exception e) {
            log.error("==> MQ：延时删除 Redis 笔记缓存消息发送失败, noteId: {}, userId: {}", noteId, userId, e);
        }
    }

    /**
     * 通知计数服务：发布者发布笔记数 +1（异步，失败仅记日志）.
     */
    private void sendNoteOperateMq(Long creatorId, Long noteId) {
        NoteOperateMqDTO dto = NoteOperateMqDTO.builder()
                .creatorId(creatorId)
                .noteId(noteId)
                .type(NoteOperateEnum.PUBLISH.getCode())
                .build();
        Message<String> message = MessageBuilder.withPayload(JsonUtils.toJsonString(dto)).build();
        String destination = MQConstants.TOPIC_NOTE_OPERATE + ":" + MQConstants.TAG_NOTE_PUBLISH;
        rocketMQTemplate.asyncSend(destination, message, new SendCallback() {
            @Override
            public void onSuccess(SendResult sendResult) {
                log.info("==> 【笔记发布：计数】MQ 发送成功, SendResult: {}", sendResult);
            }

            @Override
            public void onException(Throwable throwable) {
                log.error("==> 【笔记发布：计数】MQ 发送异常: ", throwable);
            }
        });
    }

    /**
     * 通知搜索服务：重建笔记 ES 文档（顺序发送，hashKey=noteId）.
     */
    private void sendNoteSyncEsMq(Long noteId) {
        Message<String> message = MessageBuilder.withPayload(String.valueOf(noteId)).build();
        String destination = MQConstants.TOPIC_NOTE_SYNC_ES + ":" + MQConstants.TAG_SYNC_ES_REBUILD;
        rocketMQTemplate.asyncSendOrderly(destination, message, String.valueOf(noteId), new SendCallback() {
            @Override
            public void onSuccess(SendResult sendResult) {
                log.info("==> 【笔记：ES 同步-rebuild】MQ 发送成功, noteId: {}", noteId);
            }

            @Override
            public void onException(Throwable throwable) {
                log.error("==> 【笔记：ES 同步-rebuild】MQ 发送异常, noteId: {}", noteId, throwable);
            }
        });
    }
}
