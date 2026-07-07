package com.hanserwei.gateway.filter;

import com.hanserwei.framework.common.constant.GlobalConstants;
import com.hanserwei.gateway.auth.JwtTokenHelper;
import com.hanserwei.gateway.auth.PathAuthorizationRules;
import com.hanserwei.gateway.enums.ResponseCodeEnum;
import com.hanserwei.gateway.exception.ForbiddenException;
import com.hanserwei.gateway.exception.UnauthorizedException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NullMarked;
import org.springframework.core.Ordered;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.http.HttpHeaders;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

import java.util.List;

/**
 * 网关统一鉴权过滤器.
 *
 * <p>整合登录校验、登出黑名单校验、权限校验与用户 ID 透传：
 * <ol>
 *   <li>白名单路径（登录 / 验证码）直接放行；</li>
 *   <li>读取 {@code Authorization: Bearer <jwt>}，缺失 / 无效 → 抛未登录异常；</li>
 *   <li>校验 Redis 黑名单（已登出令牌）→ 命中抛未登录异常；</li>
 *   <li>按路径规则校验角色（角色取自 JWT，避免额外查库）；</li>
 *   <li>解析用户 ID 写入下游请求头 {@code userId} 透传。</li>
 * </ol>
 *
 * <p>Redis 校验使用 {@link ReactiveStringRedisTemplate} 保持 WebFlux 全链路非阻塞。
 *
 * @author hanserwei
 * @date 2026/07/07
 * @since 0.0.1
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AuthenticationFilter implements WebFilter, Ordered {

    private static final String BEARER_PREFIX = "Bearer ";

    /** 登出黑名单 Key 前缀（与认证服务 RedisKeyConstants 保持一致） */
    private static final String TOKEN_BLACKLIST_KEY_PREFIX = "hannote:token:blacklist:";

    private final JwtTokenHelper jwtTokenHelper;
    private final ReactiveStringRedisTemplate reactiveStringRedisTemplate;
    private final PathAuthorizationRules pathAuthorizationRules;

    @Override
    @NullMarked
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String path = request.getPath().value();

        // 1. 白名单放行（登录、验证码等无需鉴权的接口）
        if (pathAuthorizationRules.isWhitelisted(path)) {
            return chain.filter(exchange);
        }

        // 2. 提取令牌
        String authorization = request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        if (authorization == null || !authorization.startsWith(BEARER_PREFIX)) {
            return Mono.error(new UnauthorizedException(ResponseCodeEnum.UNAUTHORIZED));
        }
        String token = authorization.substring(BEARER_PREFIX.length());

        // 3. 校验签名与有效期
        if (!jwtTokenHelper.validate(token)) {
            return Mono.error(new UnauthorizedException(ResponseCodeEnum.TOKEN_INVALID));
        }

        // 4. 校验黑名单（已登出令牌）
        String blacklistKey = TOKEN_BLACKLIST_KEY_PREFIX + token;
        return reactiveStringRedisTemplate.hasKey(blacklistKey)
                .flatMap(blacklisted -> {
                    if (blacklisted) {
                        return Mono.error(new UnauthorizedException(ResponseCodeEnum.UNAUTHORIZED));
                    }

                    // 5. 权限校验（角色取自 JWT）
                    List<String> requiredRoles = pathAuthorizationRules.requiredRoles(path);
                    if (!requiredRoles.isEmpty()) {
                        List<String> userRoles = jwtTokenHelper.getRoles(token);
                        boolean allowed = userRoles != null && userRoles.stream().anyMatch(requiredRoles::contains);
                        if (!allowed) {
                            return Mono.error(new ForbiddenException());
                        }
                    }

                    // 6. 透传用户 ID 到下游服务
                    Long userId = jwtTokenHelper.getUserId(token);
                    ServerWebExchange mutated = exchange.mutate()
                            .request(builder -> builder.header(GlobalConstants.USER_ID, String.valueOf(userId)))
                            .build();
                    return chain.filter(mutated);
                });
    }

    @Override
    public int getOrder() {
        // 优先于路由转发执行
        return Ordered.HIGHEST_PRECEDENCE;
    }
}
