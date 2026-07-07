package com.hanserwei.gateway.exception;

import com.hanserwei.framework.common.response.Response;
import com.hanserwei.framework.common.util.JsonUtils;
import com.hanserwei.gateway.enums.ResponseCodeEnum;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NullMarked;
import org.springframework.boot.webflux.error.ErrorWebExceptionHandler;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.buffer.DataBufferFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;

/**
 * 网关全局异常处理器.
 *
 * <p>WebFlux 环境下通过实现 {@link ErrorWebExceptionHandler} 统一异常出参格式
 * （与全局 {@link Response} 结构一致），替代 Servlet 的 {@code @ControllerAdvice}。
 *
 * <p>分类型处理：
 * <ul>
 *   <li>{@link UnauthorizedException}（未登录 / 令牌无效 / 已登出）→ 401；</li>
 *   <li>{@link ForbiddenException}（权限不足）→ 403；</li>
 *   <li>其他 → 500「网关繁忙」。</li>
 * </ul>
 * 通过 {@code @Order(-1)} 保证优先于 Spring 默认的错误处理器。
 *
 * @author hanserwei
 * @date 2026/07/07
 * @since 0.0.1
 */
@Slf4j
@Component
@Order(-1)
public class GlobalExceptionHandler implements ErrorWebExceptionHandler {

    @Override
    @NullMarked
    public Mono<Void> handle(ServerWebExchange exchange, Throwable ex) {
        ServerHttpResponse response = exchange.getResponse();
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);

        HttpStatus status;
        Response<?> body;

        switch (ex) {
            case UnauthorizedException e -> {
                status = HttpStatus.UNAUTHORIZED;
                body = Response.fail(e.getErrorCode(), e.getMessage());
            }
            case ForbiddenException e -> {
                status = HttpStatus.FORBIDDEN;
                body = Response.fail(e.getErrorCode(), e.getMessage());
            }
            default -> {
                log.error("==> 网关全局异常捕获: ", ex);
                status = HttpStatus.INTERNAL_SERVER_ERROR;
                body = Response.fail(ResponseCodeEnum.SYSTEM_ERROR);
            }
        }

        response.setStatusCode(status);
        DataBufferFactory bufferFactory = response.bufferFactory();
        byte[] bytes = JsonUtils.toJsonString(body).getBytes(StandardCharsets.UTF_8);
        return response.writeWith(Mono.just(bufferFactory.wrap(bytes)));
    }
}
