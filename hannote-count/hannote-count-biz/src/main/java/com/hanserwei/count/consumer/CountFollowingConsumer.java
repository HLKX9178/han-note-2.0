package com.hanserwei.count.consumer;

import com.hanserwei.count.constant.MQConstants;
import com.hanserwei.count.constant.RedisKeyConstants;
import com.hanserwei.count.enums.FollowUnfollowTypeEnum;
import com.hanserwei.count.model.dto.CountFollowUnfollowMqDTO;
import com.hanserwei.framework.common.util.JsonUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.rocketmq.client.producer.SendCallback;
import org.apache.rocketmq.client.producer.SendResult;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Component;

import java.util.Objects;

/**
 * 计数：关注数消费者.
 *
 * <p>关注数并发低（单用户无法短时间内关注大量用户），无需聚合，直接对 Redis Hash 的
 * {@code followingTotal} 字段 +1/-1（仅当 key 存在），随后转发落库 MQ。
 *
 * @author hanserwei
 * @date 2026/07/11
 * @since 0.0.1
 */
@Slf4j
@Component
@RequiredArgsConstructor
@RocketMQMessageListener(
        consumerGroup = MQConstants.GROUP_COUNT_FOLLOWING,
        topic = MQConstants.TOPIC_COUNT_FOLLOWING)
public class CountFollowingConsumer implements RocketMQListener<String> {

    private final RedisTemplate<String, Object> redisTemplate;
    private final RocketMQTemplate rocketMQTemplate;

    @Override
    public void onMessage(String body) {
        log.info("## 消费到 MQ 【计数：关注数】: {}", body);
        if (StringUtils.isBlank(body)) {
            return;
        }

        CountFollowUnfollowMqDTO dto = JsonUtils.parseObject(body, CountFollowUnfollowMqDTO.class);
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

        // 转发落库 MQ（原样透传消息体）
        Message<String> message = MessageBuilder.withPayload(body).build();
        rocketMQTemplate.asyncSend(MQConstants.TOPIC_COUNT_FOLLOWING_2_DB, message, new SendCallback() {
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
