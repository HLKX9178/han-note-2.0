package com.hanserwei.count.consumer;

import com.google.common.util.concurrent.RateLimiter;
import com.hanserwei.count.constant.MQConstants;
import com.hanserwei.count.domain.mapper.UserCountDOMapper;
import com.hanserwei.count.enums.FollowUnfollowTypeEnum;
import com.hanserwei.count.model.dto.CountFollowUnfollowMqDTO;
import com.hanserwei.framework.common.util.JsonUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.stereotype.Component;

import java.util.Objects;

/**
 * 计数：关注数落库消费者.
 *
 * <p>Guava 令牌桶（5000/s）削峰后，将关注数增量 upsert 到 {@code t_user_count.following_total}。
 *
 * @author hanserwei
 * @date 2026/07/11
 * @since 0.0.1
 */
@Slf4j
@Component
@RequiredArgsConstructor
@RocketMQMessageListener(
        consumerGroup = MQConstants.GROUP_COUNT_FOLLOWING_2_DB,
        topic = MQConstants.TOPIC_COUNT_FOLLOWING_2_DB)
public class CountFollowing2DBConsumer implements RocketMQListener<String> {

    private final UserCountDOMapper userCountDOMapper;

    /** 每秒 5000 个令牌，写库限流削峰 */
    private final RateLimiter rateLimiter = RateLimiter.create(5000);

    @Override
    public void onMessage(String body) {
        // 流量削峰：无可用令牌时阻塞直到获得
        rateLimiter.acquire();

        log.info("## 消费到 MQ 【计数：关注数入库】: {}", body);
        if (StringUtils.isBlank(body)) {
            return;
        }

        CountFollowUnfollowMqDTO dto = JsonUtils.parseObject(body, CountFollowUnfollowMqDTO.class);
        if (Objects.isNull(dto)) {
            return;
        }

        // 关注 +1，取关 -1
        int count = Objects.equals(dto.getType(), FollowUnfollowTypeEnum.FOLLOW.getCode()) ? 1 : -1;
        userCountDOMapper.insertOrUpdateFollowingTotalByUserId(count, dto.getUserId());
    }
}
