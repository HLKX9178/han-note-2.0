package com.hanserwei.id.controller;

import com.hanserwei.framework.common.response.Response;
import com.hanserwei.id.api.DistributedIdHttpApi;
import com.hanserwei.id.service.DistributedIdService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.RestController;

/**
 * 分布式 ID 生成控制器.
 *
 * <p>实现 {@link DistributedIdHttpApi} 契约，端点路径由契约的 {@code @PostExchange} 定义。
 *
 * @author hanserwei
 * @date 2026/07/08
 * @since 0.0.1
 */
@Slf4j
@RestController
@RequiredArgsConstructor
public class DistributedIdController implements DistributedIdHttpApi {

    private final DistributedIdService distributedIdService;

    @Override
    public Response<Long> generateHannoteId() {
        return Response.success(distributedIdService.generateHannoteId());
    }

    @Override
    public Response<Long> generateUserId() {
        return Response.success(distributedIdService.generateUserId());
    }

    @Override
    public Response<Long> generateNoteId() {
        return Response.success(distributedIdService.generateNoteId());
    }
}
