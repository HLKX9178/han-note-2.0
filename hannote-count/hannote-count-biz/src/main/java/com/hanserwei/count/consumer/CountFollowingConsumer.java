package com.hanserwei.count.consumer;

import com.hanserwei.count.constant.MQConstants;
import com.hanserwei.count.constant.RedisKeyConstants;
import com.hanserwei.count.enums.FollowUnfollowTypeEnum;
import com.hanserwei.count.model.dto.CountFollowUnfollowMqDTO;
import com.hanserwei.count.util.FollowUnfollowSourceParser;
import com.hanserwei.framework.common.util.JsonUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.client.producer.SendCallback;
import org.apache.rocketmq.client.producer.SendResult;
import org.apache.rocketmq.common.message.MessageExt;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.Objects;

/**
 * 计数：关注数消费者（并行直消费源 Topic）.
 *
 * <p>关注数并发低（单用户无法短时间内关注大量用户），无需聚合，直接对 Redis Hash 的
 * {@code followingTotal} 字段 +1/-1（仅当 key 存在），随后转发落库 MQ。
 *
 * <p>已改为**并行直消费源 Topic** {@link MQConstants#TOPIC_FOLLOW_OR_UNFOLLOW}（与 relation 落库
 * 消费者并行）。源体按 Tag 区分，经 {@link FollowUnfollowSourceParser} 归一化为
 * {@link CountFollowUnfollowMqDTO} 后沿用原计数逻辑；转发落库仍用归一化后的计数 DTO 体。
 * 幂等门移除的短时计数漂移由 hannote-data-align 日次纠偏自愈。
 *
 * @author hanserwei
 * @date 2026/07/14
 * @since 0.0.1
 */
@Slf4j
@Component
@RequiredArgsConstructor
@RocketMQMessageListener(
        consumerGroup = MQConstants.GROUP_COUNT_FOLLOWING,
        topic = MQConstants.TOPIC_FOLLOW_OR_UNFOLLOW)
public class CountFollowingConsumer implements RocketMQListener<MessageExt> {

    private final RedisTemplate<String, Object> redisTemplate;
    private final RocketMQTemplate rocketMQTemplate;

    @Override
    public void onMessage(MessageExt message) {
        String body = new String(message.getBody(), StandardCharsets.UTF_8);
        String tags = message.getTags();
        log.info("## 消费到 MQ 【计数：关注数】源事件: {}, tags: {}", body, tags);

        // 源体按 Tag 归一化为计数 DTO
        CountFollowUnfollowMqDTO dto = FollowUnfollowSourceParser.parse(tags, body);
        if (Objects.isNull(dto)) {
            return;
        }

        Long userId = dto.getUserId();
        Integer type = dto.getType();

        // 仅当 Hash 已存在时更新（缓存未初始化/已过期则跳过，以落库为准）
        String redisKey = RedisKeyConstants.buildCountUserKey(userId);
        if (Boolean.TRUE.equals(redisTemplate.hasKey(redisKey))) {
            long count = Objects.equals(type, FollowUnfollowTypeEnum.FOLLOW.getCode()) ? 1 : -1;
            redisTemplate.opsForHash().increment(redisKey, RedisKeyConstants.FIELD_FOLLOWING_TOTAL, count);
        }

        // 转发落库 MQ（归一化后的计数 DTO 体，2DB 消费者按此解析）
        Message<String> outMessage = MessageBuilder.withPayload(JsonUtils.toJsonString(dto)).build();
        rocketMQTemplate.asyncSend(MQConstants.TOPIC_COUNT_FOLLOWING_2_DB, outMessage, new SendCallback() {
            @Override
            public void onSuccess(SendResult sendResult) {
                log.info("==> 【计数服务：关注数入库】MQ 发送成功, SendResult: {}", sendResult);
            }

            @Override
            public void onException(Throwable throwable) {
                log.error("==> 【计数服务：关注数入库】MQ 发送异常: ", throwable);
            }
        });
    }
}
