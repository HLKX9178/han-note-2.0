package com.hanserwei.framework.rpc.config;

import com.hanserwei.framework.rpc.interceptor.UserIdRelayInterceptor;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.cloud.client.loadbalancer.LoadBalancerInterceptor;
import org.springframework.context.annotation.Bean;

/**
 * 服务间调用（RPC）自动配置.
 *
 * <p>声明 userId 透传拦截器与负载均衡分组配置器，使引入本 starter 的服务无需任何额外
 * 配置即可通过 {@code @ImportHttpServices} 声明并调用其他服务（HTTP Interface + LoadBalancer）。
 * {@link LoadBalancerInterceptor} 由 Spring Cloud LoadBalancer 自动装配提供。
 *
 * @author hanserwei
 * @date 2026/07/07
 * @since 0.0.1
 */
@AutoConfiguration
public class RpcAutoConfiguration {

    @Bean
    public UserIdRelayInterceptor userIdRelayInterceptor() {
        return new UserIdRelayInterceptor();
    }

    @Bean
    public LoadBalancedRestClientConfigurer loadBalancedRestClientConfigurer(
            LoadBalancerInterceptor loadBalancerInterceptor,
            UserIdRelayInterceptor userIdRelayInterceptor) {
        return new LoadBalancedRestClientConfigurer(loadBalancerInterceptor, userIdRelayInterceptor);
    }
}
