package com.hanserwei.user.consumer;

import com.hanserwei.user.constant.MQConstants;
import com.hanserwei.user.constant.RedisKeyConstants;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.util.Arrays;

/**
 * 延迟双删 Redis 用户缓存消费者（集群模式）.
 *
 * <p>用户信息更新接口在「先删缓存 → 更新库」后异步发延时消息，约 1s 后由本消费者
 * 二次删除用户信息缓存与主页信息缓存，避免并发下查询把旧数据回填缓存导致脏读。
 *
 * @author hanserwei
 * @date 2026/07/17
 * @since 0.0.1
 */
@Slf4j
@Component
@RequiredArgsConstructor
@RocketMQMessageListener(
        consumerGroup = MQConstants.GROUP_DELAY_DELETE_USER_REDIS_CACHE,
        topic = MQConstants.TOPIC_DELAY_DELETE_USER_REDIS_CACHE)
public class DelayDeleteUserRedisCacheConsumer implements RocketMQListener<String> {

    private final RedisTemplate<String, Object> redisTemplate;

    @Override
    public void onMessage(String body) {
        Long userId = Long.valueOf(body);
        log.info("## 延时消费：二次删除 Redis 用户缓存, userId: {}", userId);
        redisTemplate.delete(Arrays.asList(
                RedisKeyConstants.buildUserInfoKey(userId),
                RedisKeyConstants.buildUserProfileKey(userId)));
    }
}
