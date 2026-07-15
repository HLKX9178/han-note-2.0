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

    private double rateLimit = 1000D;
    private int batchSize = 30;
    private int maxReconsumeTimes = 3;
}
