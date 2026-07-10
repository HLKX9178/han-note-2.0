package com.hanserwei.relation.rpc;

import com.hanserwei.framework.common.response.Response;
import com.hanserwei.user.api.UserHttpApi;
import com.hanserwei.user.api.dto.req.FindUserByIdReqDTO;
import com.hanserwei.user.api.dto.req.FindUsersByIdsReqDTO;
import com.hanserwei.user.api.dto.resp.FindUserByIdRspDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * 用户服务调用封装.
 *
 * <p>通过 {@link UserHttpApi} 查询用户信息，用于关注接口校验「被关注用户是否真实存在」。
 *
 * @author hanserwei
 * @date 2026/07/10
 * @since 0.0.1
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class UserRpcService {

    private final UserHttpApi userHttpApi;

    /**
     * 根据用户 ID 查询用户信息.
     *
     * @param userId 用户 ID
     * @return 用户信息；查询失败或用户不存在返回 {@code null}
     */
    public FindUserByIdRspDTO findById(Long userId) {
        FindUserByIdReqDTO findUserByIdReqDTO = FindUserByIdReqDTO.builder()
                .id(userId)
                .build();

        Response<FindUserByIdRspDTO> response = userHttpApi.findById(findUserByIdReqDTO);
        if (Objects.isNull(response) || !response.isSuccess()) {
            log.error("==> 调用用户服务查询用户信息失败, userId: {}, response: {}", userId, response);
            return null;
        }
        return response.getData();
    }

    /**
     * 批量根据用户 ID 查询用户信息.
     *
     * <p>供关注/粉丝列表接口将 ZSET / 数据库中的 userId 批量换成用户信息。单次批量上限为 10
     * （由 {@link FindUsersByIdsReqDTO} 的 {@code @Size} 约束），恰好匹配每页 10 条。
     *
     * @param userIds 用户 ID 集合（大小 [1, 10]）
     * @return 用户信息列表；查询失败或无数据返回空列表
     */
    public List<FindUserByIdRspDTO> findByIds(List<Long> userIds) {
        FindUsersByIdsReqDTO findUsersByIdsReqDTO = FindUsersByIdsReqDTO.builder()
                .ids(userIds)
                .build();

        Response<List<FindUserByIdRspDTO>> response = userHttpApi.findByIds(findUsersByIdsReqDTO);
        if (Objects.isNull(response) || !response.isSuccess() || Objects.isNull(response.getData())) {
            log.error("==> 调用用户服务批量查询用户信息失败, userIds: {}, response: {}", userIds, response);
            return Collections.emptyList();
        }
        return response.getData();
    }
}
