package com.hanserwei.note.consumer;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.google.common.util.concurrent.RateLimiter;
import com.hanserwei.framework.common.util.JsonUtils;
import com.hanserwei.note.constant.MQConstants;
import com.hanserwei.note.domain.dataobject.NoteLikeDO;
import com.hanserwei.note.domain.mapper.NoteLikeDOMapper;
import com.hanserwei.note.enums.LikeUnlikeNoteTypeEnum;
import com.hanserwei.note.model.dto.LikeUnlikeNoteMqDTO;
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
 * 笔记点赞 / 取消点赞 MQ 消费者（顺序消费）.
 *
 * <p>消费 {@link MQConstants#TOPIC_LIKE_UNLIKE}，按 Tag 分派：{@code Like} 新增/更新点赞记录，
 * {@code Unlike} 将点赞记录 status 置 0。落库成功后转发 {@link MQConstants#TOPIC_COUNT_NOTE_LIKE}
 * 通知计数服务。
 *
 * <p>削峰：Guava 令牌桶阻塞式获取令牌，以数据库可承受速率消费。
 * 幂等：点赞 upsert 的状态守卫 + 取消点赞的 {@code status=1} 更新条件，避免重复消费导致重复计数。
 *
 * @author hanserwei
 * @date 2026/07/11
 * @since 0.0.1
 */
@Slf4j
@Component
@RequiredArgsConstructor
@RocketMQMessageListener(
        consumerGroup = MQConstants.GROUP_LIKE_UNLIKE,
        topic = MQConstants.TOPIC_LIKE_UNLIKE,
        consumeMode = ConsumeMode.ORDERLY)
public class LikeUnlikeNoteConsumer implements RocketMQListener<MessageExt> {

    private final NoteLikeDOMapper noteLikeDOMapper;
    private final RocketMQTemplate rocketMQTemplate;

    /** 令牌桶削峰：每秒 5000 个令牌 */
    private final RateLimiter rateLimiter = RateLimiter.create(5000);

    @Override
    public void onMessage(MessageExt message) {
        // 流量削峰：获取令牌，无可用令牌时阻塞直到获得
        rateLimiter.acquire();

        String bodyJsonStr = new String(message.getBody(), StandardCharsets.UTF_8);
        String tags = message.getTags();
        log.info("==> LikeUnlikeNoteConsumer 消费消息: {}, tags: {}", bodyJsonStr, tags);

        if (Objects.equals(tags, MQConstants.TAG_LIKE)) {
            handleLikeTagMessage(bodyJsonStr);
        } else if (Objects.equals(tags, MQConstants.TAG_UNLIKE)) {
            handleUnlikeTagMessage(bodyJsonStr);
        }
    }

    /**
     * 点赞落库：upsert t_note_like（status=1）。
     *
     * @param bodyJsonStr 消息体 JSON
     */
    private void handleLikeTagMessage(String bodyJsonStr) {
        LikeUnlikeNoteMqDTO dto = JsonUtils.parseObject(bodyJsonStr, LikeUnlikeNoteMqDTO.class);
        if (Objects.isNull(dto)) {
            return;
        }

        NoteLikeDO noteLikeDO = NoteLikeDO.builder()
                .userId(dto.getUserId())
                .noteId(dto.getNoteId())
                .createTime(dto.getCreateTime())
                .status(LikeUnlikeNoteTypeEnum.LIKE.getCode())
                .build();
        int count = noteLikeDOMapper.insertOrUpdateLike(noteLikeDO);

        // 仅当确实发生点赞状态变更（幂等守卫下 count>0）才转发计数，避免重复投递重复 +1
        if (count > 0) {
            sendCountMQ(bodyJsonStr);
        }
    }

    /**
     * 取消点赞落库：仅将已点赞（status=1）的记录置为 0。
     *
     * @param bodyJsonStr 消息体 JSON
     */
    private void handleUnlikeTagMessage(String bodyJsonStr) {
        LikeUnlikeNoteMqDTO dto = JsonUtils.parseObject(bodyJsonStr, LikeUnlikeNoteMqDTO.class);
        if (Objects.isNull(dto)) {
            return;
        }

        // status=1 条件：仅已点赞记录可被取消，防止布隆误判把不存在/已取消的记录改错
        int count = noteLikeDOMapper.update(null, new LambdaUpdateWrapper<NoteLikeDO>()
                .eq(NoteLikeDO::getUserId, dto.getUserId())
                .eq(NoteLikeDO::getNoteId, dto.getNoteId())
                .eq(NoteLikeDO::getStatus, LikeUnlikeNoteTypeEnum.LIKE.getCode())
                .set(NoteLikeDO::getStatus, LikeUnlikeNoteTypeEnum.UNLIKE.getCode())
                .set(NoteLikeDO::getCreateTime, dto.getCreateTime()));

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
        rocketMQTemplate.asyncSend(MQConstants.TOPIC_COUNT_NOTE_LIKE, message, new SendCallback() {
            @Override
            public void onSuccess(SendResult sendResult) {
                log.info("==> 【计数：笔记点赞数】MQ 发送成功, SendResult: {}", sendResult);
            }

            @Override
            public void onException(Throwable throwable) {
                log.error("==> 【计数：笔记点赞数】MQ 发送异常: ", throwable);
            }
        });
    }
}
