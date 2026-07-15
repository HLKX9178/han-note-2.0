package com.hanserwei.comment.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 发送 MQ 重试配置.
 *
 * @author hanserwei
 * @date 2026/07/14
 * @since 0.0.1
 */
@ConfigurationProperties(prefix = RetryProperties.PREFIX)
@Component
@Data
public class RetryProperties {

    public static final String PREFIX = "retry";

    /** 最大重试次数 */
    private Integer maxAttempts = 3;
    /** 初始间隔（ms） */
    private Integer initInterval = 1000;
    /** 每次间隔乘数 */
    private Double multiplier = 2.0;
}
