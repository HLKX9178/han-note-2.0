package com.hanserwei.user.config;

import com.hanserwei.oss.api.FileHttpApi;
import com.hanserwei.oss.api.constant.OssApiConstants;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.service.registry.ImportHttpServices;

/**
 * RPC 客户端声明.
 *
 * <p>把 oss 服务的 {@link FileHttpApi} 注册为分组 {@code hannote-oss} 的 HTTP Interface 客户端。
 * RPC starter 的 {@code LoadBalancedRestClientConfigurer} 会自动为该分组设置
 * {@code baseUrl = lb://hannote-oss} 并挂上负载均衡与 userId 透传拦截器。
 *
 * @author hanserwei
 * @date 2026/07/07
 * @since 0.0.1
 */
@Configuration
@ImportHttpServices(group = OssApiConstants.SERVICE_NAME, types = FileHttpApi.class)
public class RpcClientConfig {
}
