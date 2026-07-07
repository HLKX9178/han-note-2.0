package com.hanserwei.framework.rpc.config;

import com.hanserwei.framework.rpc.interceptor.UserIdRelayInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.cloud.client.loadbalancer.LoadBalancerInterceptor;
import org.springframework.web.client.support.RestClientHttpServiceGroupConfigurer;

/**
 * 负载均衡 + userId 透传的 RestClient 分组配置器.
 *
 * <p>对每个 {@code @ImportHttpServices} 分组，将 RestClient 的 baseUrl 设为
 * {@code lb://<分组名>}（分组名即目标 Nacos 服务名），并依次挂上：
 * <ol>
 *   <li>{@link LoadBalancerInterceptor} —— 把 {@code lb://服务名} 解析为真实实例地址；</li>
 *   <li>{@link UserIdRelayInterceptor} —— 透传当前登录用户 ID 到下游。</li>
 * </ol>
 *
 * @author hanserwei
 * @date 2026/07/07
 * @since 0.0.1
 */
@RequiredArgsConstructor
public class LoadBalancedRestClientConfigurer implements RestClientHttpServiceGroupConfigurer {

    private final LoadBalancerInterceptor loadBalancerInterceptor;
    private final UserIdRelayInterceptor userIdRelayInterceptor;

    @Override
    public void configureGroups(Groups<org.springframework.web.client.RestClient.Builder> groups) {
        groups.forEachClient((group, builder) -> builder
                .baseUrl("lb://" + group.name())
                .requestInterceptor(loadBalancerInterceptor)
                .requestInterceptor(userIdRelayInterceptor));
    }
}
