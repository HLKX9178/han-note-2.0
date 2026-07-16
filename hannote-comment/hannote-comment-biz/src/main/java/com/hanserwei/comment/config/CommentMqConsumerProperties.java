package com.hanserwei.comment.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 评论互动 MQ 消费参数.
 *
 * @author hanserwei
 * @date 2026/07/15
 * @since 0.0.1
 */
@Data
@Component
@ConfigurationProperties(prefix = "comment.mq-consumer.like-unlike")
public class CommentMqConsumerProperties {

    /** 令牌桶限流速率（每秒放行消息数），按数据库承受力调整 */
    private double rateLimit = 1000D;
    /** 单批最大消费条数 */
    private int batchSize = 30;
    /** 消费失败最大重试次数 */
    private int maxReconsumeTimes = 3;
}
