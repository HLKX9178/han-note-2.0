package com.hanserwei.note.rpc;

import com.hanserwei.framework.common.response.Response;
import com.hanserwei.user.api.UserHttpApi;
import com.hanserwei.user.api.dto.req.FindUserByIdReqDTO;
import com.hanserwei.user.api.dto.resp.FindUserByIdRspDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Objects;

/**
 * 用户服务调用封装.
 *
 * <p>通过 {@link UserHttpApi} 查询发布者信息（昵称 / 头像），供笔记详情展示。
 *
 * @author hanserwei
 * @date 2026/07/09
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
     * @return 用户信息；失败返回 {@code null}
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
}
