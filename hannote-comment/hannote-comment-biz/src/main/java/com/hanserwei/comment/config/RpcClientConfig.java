package com.hanserwei.comment.config;

import com.hanserwei.id.api.DistributedIdHttpApi;
import com.hanserwei.id.api.constant.IdApiConstants;
import com.hanserwei.kv.api.KeyValueHttpApi;
import com.hanserwei.kv.api.constant.KvApiConstants;
import com.hanserwei.note.api.NoteHttpApi;
import com.hanserwei.note.api.constant.NoteApiConstants;
import com.hanserwei.count.api.CountHttpApi;
import com.hanserwei.count.api.constant.CountApiConstants;
import com.hanserwei.user.api.UserHttpApi;
import com.hanserwei.user.api.constant.UserApiConstants;
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
@ImportHttpServices(group = NoteApiConstants.SERVICE_NAME, types = NoteHttpApi.class)
@ImportHttpServices(group = CountApiConstants.SERVICE_NAME, types = CountHttpApi.class)
@ImportHttpServices(group = UserApiConstants.SERVICE_NAME, types = UserHttpApi.class)
public class RpcClientConfig {
}
