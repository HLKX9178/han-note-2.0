package com.hanserwei.relation.config;

import com.google.common.util.concurrent.RateLimiter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 关注 / 取关 MQ 消费者令牌桶配置.
 *
 * <p>令牌生成速率（每秒令牌数）读自配置项 {@code mq-consumer.follow-unfollow.rate-limit}，
 * 并托管于 Nacos 配置中心。配合 {@link RefreshScope}：在 Nacos 动态修改阈值并发布后，
 * 该 Bean 会被销毁重建，{@link RateLimiter} 以新速率重新初始化，实现集群扩缩容时
 * 动态调整每实例限流阈值，无需重启服务。
 *
 * @author hanserwei
 * @date 2026/07/10
 * @since 0.0.1
 */
@Configuration
public class FollowUnfollowMqConsumerRateLimitConfig {

    /** 每秒生成的令牌数（限流阈值），默认 5000 */
    @Value("${mq-consumer.follow-unfollow.rate-limit:5000}")
    private double rateLimit;

    /**
     * 令牌桶限流器.
     *
     * @return 以 {@code rateLimit} 为每秒速率的 {@link RateLimiter}
     */
    @Bean
    @RefreshScope
    public RateLimiter followUnfollowRateLimiter() {
        return RateLimiter.create(rateLimit);
    }
}
