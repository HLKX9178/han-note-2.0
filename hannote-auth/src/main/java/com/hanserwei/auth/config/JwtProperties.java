package com.hanserwei.auth.config;

import jakarta.annotation.PostConstruct;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;

/**
 * JWT 配置属性.
 *
 * <p>绑定 {@code application-dev.yml} 中 {@code hannote.jwt.*} 命名空间：
 * <pre>
 * hannote:
 *   jwt:
 *     secret: ${JWT_SECRET}
 *     expiration: 2592000
 * </pre>
 *
 * <p>{@link #validate()} 在 Bean 初始化后校验密钥合规性（&gt;= 32 字节），不满足即快速失败。
 *
 * @author hanserwei
 * @date 2026/07/07
 * @since 0.0.1
 */
@ConfigurationProperties(prefix = "hannote.jwt")
@Component
@Data
public class JwtProperties {

    /** 签名密钥（必须 >= 32 字节以满足 HS256 要求） */
    private String secret;

    /** Token 有效期（秒），默认 30 天 */
    private long expiration = 2592000L;

    @PostConstruct
    public void validate() {
        if (secret == null || secret.isBlank()) {
            throw new IllegalStateException(
                    "配置 hannote.jwt.secret 未设置。请在 application-dev.yml 中配置（或通过环境变量 JWT_SECRET 注入）");
        }
        if (secret.getBytes(StandardCharsets.UTF_8).length < 32) {
            throw new IllegalStateException(
                    "配置 hannote.jwt.secret 长度必须 >= 32 字节（HS256 要求），当前长度："
                            + secret.getBytes(StandardCharsets.UTF_8).length);
        }
    }
}
