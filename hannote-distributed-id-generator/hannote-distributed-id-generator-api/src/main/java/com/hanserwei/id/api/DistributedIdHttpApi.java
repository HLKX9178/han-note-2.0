package com.hanserwei.id.api;

import com.hanserwei.framework.common.response.Response;
import org.springframework.web.service.annotation.HttpExchange;
import org.springframework.web.service.annotation.PostExchange;

/**
 * 分布式 ID 生成服务对外契约（HTTP Interface）.
 *
 * <p>供其他服务通过 {@code @ImportHttpServices(group = IdApiConstants.SERVICE_NAME,
 * types = DistributedIdHttpApi.class)} 声明并注入调用。仅供内网服务间 RPC 使用，
 * 不经网关对外暴露。
 *
 * @author hanserwei
 * @date 2026/07/08
 * @since 0.0.1
 */
@HttpExchange
public interface DistributedIdHttpApi {

    /** ID 服务上下文前缀（与 DistributedIdController 的 @RequestMapping 对齐） */
    String PREFIX = "/id";

    /**
     * 生成小憨书 ID（hannote-id）.
     *
     * @return 全局唯一的小憨书 ID
     */
    @PostExchange(PREFIX + "/hannote/generate")
    Response<Long> generateHannoteId();

    /**
     * 生成用户 ID（user-id）.
     *
     * @return 全局唯一的用户 ID
     */
    @PostExchange(PREFIX + "/user/generate")
    Response<Long> generateUserId();
}
