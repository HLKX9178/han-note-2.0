package com.hanserwei.framework.rpc.interceptor;

import com.hanserwei.framework.biz.context.holder.LoginUserContextHolder;
import com.hanserwei.framework.common.constant.GlobalConstants;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;

import java.io.IOException;
import java.util.Objects;

/**
 * 服务间调用 userId 透传拦截器.
 *
 * <p>服务间调用不经过网关，下游服务无法从请求头拿到 userId。本拦截器在发起 RestClient
 * 请求前，从 {@link LoginUserContextHolder} 取当前 userId 写入请求头，使下游服务的
 * 上下文过滤器可再次解析。等价源教程的 Feign {@code RequestInterceptor}。
 *
 * @author hanserwei
 * @date 2026/07/07
 * @since 0.0.1
 */
@Slf4j
public class UserIdRelayInterceptor implements ClientHttpRequestInterceptor {

    @NonNull
    @Override
    public ClientHttpResponse intercept(@NonNull HttpRequest request, byte @NonNull [] body,
                                        @NonNull ClientHttpRequestExecution execution) throws IOException {
        Long userId = LoginUserContextHolder.getUserId();
        if (Objects.nonNull(userId)) {
            request.getHeaders().add(GlobalConstants.USER_ID, String.valueOf(userId));
            log.debug("==> RPC 请求透传 userId: {}", userId);
        }
        return execution.execute(request, body);
    }
}
