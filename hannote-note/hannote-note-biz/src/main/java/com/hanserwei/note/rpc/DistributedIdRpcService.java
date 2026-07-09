package com.hanserwei.note.rpc;

import com.hanserwei.framework.common.response.Response;
import com.hanserwei.id.api.DistributedIdHttpApi;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Objects;

/**
 * 分布式 ID 生成服务调用封装.
 *
 * <p>通过 {@link DistributedIdHttpApi} 调用 ID 服务生成笔记 ID（note-id）。
 *
 * @author hanserwei
 * @date 2026/07/09
 * @since 0.0.1
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DistributedIdRpcService {

    private final DistributedIdHttpApi distributedIdHttpApi;

    /**
     * 生成笔记 ID.
     *
     * @return 成功返回 ID；失败返回 {@code null}
     */
    public Long generateNoteId() {
        Response<Long> response = distributedIdHttpApi.generateNoteId();
        if (Objects.isNull(response) || !response.isSuccess()) {
            log.error("==> 调用分布式 ID 服务生成 note-id 失败, response: {}", response);
            return null;
        }
        return response.getData();
    }
}
