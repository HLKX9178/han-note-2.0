package com.hanserwei.auth.security;

import com.hanserwei.auth.config.JwtProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtParser;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.List;

/**
 * JWT 令牌提供者.
 *
 * <p>负责 JWT 的签发与解析，使用 HMAC-SHA256 算法（HS256）。
 * 签名密钥与有效期通过 {@link JwtProperties} 注入（要求 secret &gt;= 32 字节）。
 *
 * <p>签发载荷：
 * <ul>
 *   <li>{@code sub}：用户 ID；</li>
 *   <li>{@code phone}：手机号；</li>
 *   <li>{@code roles}：角色 Key 列表；</li>
 *   <li>{@code iat} / {@code exp}：签发时间与过期时间。</li>
 * </ul>
 *
 * @author hanserwei
 * @date 2026/07/07
 * @since 0.0.1
 */
@Slf4j
@Component
public class JwtTokenProvider {

    private final JwtProperties jwtProperties;

    /** 缓存签名密钥，避免每次签发都重新生成 */
    private volatile SecretKey signingKey;

    /** 缓存解析器，避免每次解析都重建 */
    private volatile JwtParser jwtParser;

    public JwtTokenProvider(JwtProperties jwtProperties) {
        this.jwtProperties = jwtProperties;
    }

    /**
     * 获取签名密钥（懒加载、线程安全）.
     *
     * @return HS256 签名密钥
     */
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

    /**
     * 获取解析器（懒加载、线程安全、可复用）.
     *
     * @return 已绑定签名密钥的解析器
     */
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

    /**
     * 签发 JWT.
     *
     * @param userId 用户 ID
     * @param phone  手机号
     * @param roles  角色 Key 列表
     * @return 紧凑格式的 JWT 字符串
     */
    public String generateToken(Long userId, String phone, List<String> roles) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + jwtProperties.getExpiration() * 1000L);

        return Jwts.builder()
                .subject(String.valueOf(userId))
                .claim("phone", phone)
                .claim("roles", roles)
                .issuedAt(now)
                .expiration(expiry)
                .signWith(getSigningKey())
                .compact();
    }

    /**
     * 从令牌中解析用户 ID.
     *
     * @param token JWT 字符串
     * @return 用户 ID
     */
    public Long getUserId(String token) {
        return Long.parseLong(parseClaims(token).getSubject());
    }

    /**
     * 从令牌中解析手机号.
     *
     * @param token JWT 字符串
     * @return 手机号
     */
    public String getPhone(String token) {
        return parseClaims(token).get("phone", String.class);
    }

    /**
     * 从令牌中解析角色 Key 列表.
     *
     * @param token JWT 字符串
     * @return 角色 Key 列表
     */
    @SuppressWarnings("unchecked")
    public List<String> getRoles(String token) {
        return parseClaims(token).get("roles", List.class);
    }

    /**
     * 校验令牌有效性.
     *
     * @param token JWT 字符串
     * @return 可正常解析返回 true，否则返回 false
     */
    public boolean validate(String token) {
        try {
            parseClaims(token);
            return true;
        } catch (Exception e) {
            log.warn("JWT validation failed: {}", e.getMessage());
            return false;
        }
    }

    /**
     * 解析载荷.
     *
     * @param token JWT 字符串
     * @return Claims
     */
    private Claims parseClaims(String token) {
        return getParser().parseSignedClaims(token).getPayload();
    }
}
