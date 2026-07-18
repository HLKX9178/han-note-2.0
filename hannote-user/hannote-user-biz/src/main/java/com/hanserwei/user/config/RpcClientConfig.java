package com.hanserwei.user.config;

import com.hanserwei.count.api.CountHttpApi;
import com.hanserwei.count.api.constant.CountApiConstants;
import com.hanserwei.id.api.DistributedIdHttpApi;
import com.hanserwei.id.api.constant.IdApiConstants;
import com.hanserwei.oss.api.FileHttpApi;
import com.hanserwei.oss.api.constant.OssApiConstants;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.service.registry.ImportHttpServices;

/**
 * RPC 客户端声明.
 *
 * <p>把 oss 服务的 {@link FileHttpApi}、分布式 ID 服务的 {@link DistributedIdHttpApi}、
 * 计数服务的 {@link CountHttpApi} 分别注册为对应分组的 HTTP Interface 客户端。RPC starter 的
 * {@code LoadBalancedRestClientConfigurer} 会自动为各分组设置 {@code baseUrl = lb://<group>}
 * 并挂上负载均衡与 userId 透传拦截器。
 *
 * @author hanserwei
 * @date 2026/07/07
 * @since 0.0.1
 */
@Configuration
@ImportHttpServices(group = OssApiConstants.SERVICE_NAME, types = FileHttpApi.class)
@ImportHttpServices(group = IdApiConstants.SERVICE_NAME, types = DistributedIdHttpApi.class)
@ImportHttpServices(group = CountApiConstants.SERVICE_NAME, types = CountHttpApi.class)
public class RpcClientConfig {
}
