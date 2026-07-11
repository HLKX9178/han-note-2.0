package com.hanserwei.note.consumer;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.google.common.util.concurrent.RateLimiter;
import com.hanserwei.framework.common.util.JsonUtils;
import com.hanserwei.note.constant.MQConstants;
import com.hanserwei.note.domain.dataobject.NoteCollectionDO;
import com.hanserwei.note.domain.mapper.NoteCollectionDOMapper;
import com.hanserwei.note.enums.CollectUnCollectNoteTypeEnum;
import com.hanserwei.note.model.dto.CollectUnCollectNoteMqDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.client.producer.SendCallback;
import org.apache.rocketmq.client.producer.SendResult;
import org.apache.rocketmq.common.message.MessageExt;
import org.apache.rocketmq.spring.annotation.ConsumeMode;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.Objects;

/**
 * 笔记收藏 / 取消收藏 MQ 消费者（顺序消费）.
 *
 * <p>消费 {@link MQConstants#TOPIC_COLLECT_UNCOLLECT}，按 Tag 分派：{@code Collect} 新增/更新收藏
 * 记录，{@code UnCollect} 将收藏记录 status 置 0。落库成功后转发
 * {@link MQConstants#TOPIC_COUNT_NOTE_COLLECT} 通知计数服务。处理逻辑与点赞消费者一致。
 *
 * @author hanserwei
 * @date 2026/07/11
 * @since 0.0.1
 */
@Slf4j
@Component
@RequiredArgsConstructor
@RocketMQMessageListener(
        consumerGroup = MQConstants.GROUP_COLLECT_UNCOLLECT,
        topic = MQConstants.TOPIC_COLLECT_UNCOLLECT,
        consumeMode = ConsumeMode.ORDERLY)
public class CollectUnCollectNoteConsumer implements RocketMQListener<MessageExt> {

    private final NoteCollectionDOMapper noteCollectionDOMapper;
    private final RocketMQTemplate rocketMQTemplate;

    /** 令牌桶削峰：每秒 5000 个令牌 */
    private final RateLimiter rateLimiter = RateLimiter.create(5000);

    @Override
    public void onMessage(MessageExt message) {
        // 流量削峰：获取令牌，无可用令牌时阻塞直到获得
        rateLimiter.acquire();

        String bodyJsonStr = new String(message.getBody(), StandardCharsets.UTF_8);
        String tags = message.getTags();
        log.info("==> CollectUnCollectNoteConsumer 消费消息: {}, tags: {}", bodyJsonStr, tags);

        if (Objects.equals(tags, MQConstants.TAG_COLLECT)) {
            handleCollectTagMessage(bodyJsonStr);
        } else if (Objects.equals(tags, MQConstants.TAG_UNCOLLECT)) {
            handleUnCollectTagMessage(bodyJsonStr);
        }
    }

    /**
     * 收藏落库：upsert t_note_collection（status=1）。
     *
     * @param bodyJsonStr 消息体 JSON
     */
    private void handleCollectTagMessage(String bodyJsonStr) {
        CollectUnCollectNoteMqDTO dto = JsonUtils.parseObject(bodyJsonStr, CollectUnCollectNoteMqDTO.class);
        if (Objects.isNull(dto)) {
            return;
        }

        NoteCollectionDO noteCollectionDO = NoteCollectionDO.builder()
                .userId(dto.getUserId())
                .noteId(dto.getNoteId())
                .createTime(dto.getCreateTime())
                .status(CollectUnCollectNoteTypeEnum.COLLECT.getCode())
                .build();
        int count = noteCollectionDOMapper.insertOrUpdateCollect(noteCollectionDO);

        if (count > 0) {
            sendCountMQ(bodyJsonStr);
        }
    }

    /**
     * 取消收藏落库：仅将已收藏（status=1）的记录置为 0。
     *
     * @param bodyJsonStr 消息体 JSON
     */
    private void handleUnCollectTagMessage(String bodyJsonStr) {
        CollectUnCollectNoteMqDTO dto = JsonUtils.parseObject(bodyJsonStr, CollectUnCollectNoteMqDTO.class);
        if (Objects.isNull(dto)) {
            return;
        }

        // status=1 条件：仅已收藏记录可被取消，防止布隆误判把不存在/已取消的记录改错
        int count = noteCollectionDOMapper.update(null, new LambdaUpdateWrapper<NoteCollectionDO>()
                .eq(NoteCollectionDO::getUserId, dto.getUserId())
                .eq(NoteCollectionDO::getNoteId, dto.getNoteId())
                .eq(NoteCollectionDO::getStatus, CollectUnCollectNoteTypeEnum.COLLECT.getCode())
                .set(NoteCollectionDO::getStatus, CollectUnCollectNoteTypeEnum.UN_COLLECT.getCode())
                .set(NoteCollectionDO::getCreateTime, dto.getCreateTime()));

        if (count > 0) {
            sendCountMQ(bodyJsonStr);
        }
    }

    /**
     * 转发计数 MQ（原始消息体，含 noteCreatorId）到计数服务。
     *
     * @param bodyJsonStr 原始消息体 JSON
     */
    private void sendCountMQ(String bodyJsonStr) {
        Message<String> message = MessageBuilder.withPayload(bodyJsonStr).build();
        rocketMQTemplate.asyncSend(MQConstants.TOPIC_COUNT_NOTE_COLLECT, message, new SendCallback() {
            @Override
            public void onSuccess(SendResult sendResult) {
                log.info("==> 【计数：笔记收藏数】MQ 发送成功, SendResult: {}", sendResult);
            }

            @Override
            public void onException(Throwable throwable) {
                log.error("==> 【计数：笔记收藏数】MQ 发送异常: ", throwable);
            }
        });
    }
}
