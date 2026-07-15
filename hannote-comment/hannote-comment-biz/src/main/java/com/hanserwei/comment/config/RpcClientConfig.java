package com.hanserwei.comment.config;

import com.hanserwei.id.api.DistributedIdHttpApi;
import com.hanserwei.id.api.constant.IdApiConstants;
import com.hanserwei.kv.api.KeyValueHttpApi;
import com.hanserwei.kv.api.constant.KvApiConstants;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.service.registry.ImportHttpServices;

/**
 * RPC 客户端声明（HTTP Interface + LoadBalancer）.
 *
 * @author hanserwei
 * @date 2026/07/14
 * @since 0.0.1
 */
@Configuration
@ImportHttpServices(group = IdApiConstants.SERVICE_NAME, types = DistributedIdHttpApi.class)
@ImportHttpServices(group = KvApiConstants.SERVICE_NAME, types = KeyValueHttpApi.class)
public class RpcClientConfig {
}
