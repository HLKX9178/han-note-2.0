package com.hanserwei.auth.config;

import com.hanserwei.user.api.UserHttpApi;
import com.hanserwei.user.api.constant.UserApiConstants;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.service.registry.ImportHttpServices;

/**
 * RPC 客户端声明.
 *
 * <p>把用户服务的 {@link UserHttpApi} 注册为分组 {@code hannote-user} 的 HTTP Interface 客户端。
 * RPC starter 的 {@code LoadBalancedRestClientConfigurer} 会自动为该分组设置
 * {@code baseUrl = lb://hannote-user} 并挂上负载均衡与 userId 透传拦截器。
 *
 * @author hanserwei
 * @date 2026/07/08
 * @since 0.0.1
 */
@Configuration
@ImportHttpServices(group = UserApiConstants.SERVICE_NAME, types = UserHttpApi.class)
public class RpcClientConfig {
}
