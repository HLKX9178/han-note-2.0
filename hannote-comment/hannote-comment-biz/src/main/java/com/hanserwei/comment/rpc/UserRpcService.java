package com.hanserwei.comment.rpc;

import com.hanserwei.framework.common.response.Response;
import com.hanserwei.comment.config.CommentQueryExecutor;
import com.hanserwei.user.api.UserHttpApi;
import com.hanserwei.user.api.dto.req.FindUsersByIdsReqDTO;
import com.hanserwei.user.api.dto.resp.FindUserByIdRspDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

/**
 * 用户服务批量 RPC 封装，自动按 10 个 ID 分片.
 *
 * @author hanserwei
 * @date 2026/07/15
 * @since 0.0.1
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class UserRpcService {

    private static final int BATCH_SIZE = 10;

    private final UserHttpApi userHttpApi;
    private final CommentQueryExecutor commentQueryExecutor;

    /**
     * 批量查询用户并按 ID 返回 Map；单批失败降级为空批次.
     */
    public Map<Long, FindUserByIdRspDTO> findByIds(List<Long> userIds) {
        List<Long> distinct = new ArrayList<>(new LinkedHashSet<>(userIds));
        List<CompletableFuture<List<FindUserByIdRspDTO>>> futures = new ArrayList<>();
        for (int start = 0; start < distinct.size(); start += BATCH_SIZE) {
            List<Long> batch = distinct.subList(start, Math.min(start + BATCH_SIZE, distinct.size()));
            futures.add(CompletableFuture.supplyAsync(() -> findBatch(batch), commentQueryExecutor));
        }
        return futures.stream()
                .flatMap(future -> future.join().stream())
                .collect(Collectors.toMap(FindUserByIdRspDTO::getId, item -> item, (left, right) -> left));
    }

    private List<FindUserByIdRspDTO> findBatch(List<Long> batch) {
        try {
            Response<List<FindUserByIdRspDTO>> response = userHttpApi.findByIds(
                    FindUsersByIdsReqDTO.builder().ids(batch).build());
            if (Objects.isNull(response) || !response.isSuccess() || Objects.isNull(response.getData())) {
                log.error("==> 批量查询用户失败, userIds: {}, response: {}", batch, response);
                return List.of();
            }
            return response.getData();
        } catch (Exception e) {
            log.error("==> 批量查询用户异常, userIds: {}", batch, e);
            return List.of();
        }
    }
}
