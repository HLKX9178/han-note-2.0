package com.hanserwei.gateway.auth;

import com.hanserwei.gateway.config.JwtProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtParser;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * 网关 JWT 令牌解析辅助类.
 *
 * <p>与认证服务共享同一签名密钥，负责在网关侧校验令牌签名/有效期并解析出用户 ID、角色。
 * 采用与认证服务一致的 HS256 算法，解析器懒加载并复用。
 *
 * @author hanserwei
 * @date 2026/07/07
 * @since 0.0.1
 */
@Slf4j
@Component
public class JwtTokenHelper {

    private final JwtProperties jwtProperties;
    private volatile SecretKey signingKey;
    private volatile JwtParser jwtParser;

    public JwtTokenHelper(JwtProperties jwtProperties) {
        this.jwtProperties = jwtProperties;
    }

    private SecretKey getSigningKey() {
        SecretKey key = this.signingKey;
        if (key == null) {
            synchronized (this) {
                key = this.signingKey;
                if (key == null) {
                    key = Keys.hmacShaKeyFor(jwtProperties.getSecret().getBytes(StandardCharsets.UTF_8));
                    this.signingKey = key;
                }
            }
        }
        return key;
    }

    private JwtParser getParser() {
        JwtParser parser = this.jwtParser;
        if (parser == null) {
            synchronized (this) {
                parser = this.jwtParser;
                if (parser == null) {
                    parser = Jwts.parser().verifyWith(getSigningKey()).build();
                    this.jwtParser = parser;
                }
            }
        }
        return parser;
    }

    private Claims parseClaims(String token) {
        return getParser().parseSignedClaims(token).getPayload();
    }

    /**
     * 校验令牌有效性（签名 + 是否过期）.
     *
     * @param token JWT 字符串
     * @return 有效返回 true
     */
    public boolean validate(String token) {
        try {
            parseClaims(token);
            return true;
        } catch (Exception e) {
            log.warn("网关 JWT 校验失败: {}", e.getMessage());
            return false;
        }
    }

    /**
     * 解析用户 ID.
     *
     * @param token JWT 字符串
     * @return 用户 ID
     */
    public Long getUserId(String token) {
        return Long.parseLong(parseClaims(token).getSubject());
    }

    /**
     * 解析角色 Key 列表.
     *
     * @param token JWT 字符串
     * @return 角色 Key 列表
     */
    @SuppressWarnings("unchecked")
    public List<String> getRoles(String token) {
        return parseClaims(token).get("roles", List.class);
    }
}
