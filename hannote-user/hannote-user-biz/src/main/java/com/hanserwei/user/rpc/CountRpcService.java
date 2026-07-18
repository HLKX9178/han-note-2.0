package com.hanserwei.user.rpc;

import com.hanserwei.count.api.CountHttpApi;
import com.hanserwei.count.api.dto.req.FindUserCountReqDTO;
import com.hanserwei.count.api.dto.resp.FindUserCountRspDTO;
import com.hanserwei.framework.common.response.Response;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Objects;

/**
 * 计数服务调用封装.
 *
 * <p>通过 {@link CountHttpApi} 查询用户维度计数（关注/粉丝/笔记/获赞/获藏）。
 *
 * @author hanserwei
 * @date 2026/07/17
 * @since 0.0.1
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CountRpcService {

    private final CountHttpApi countHttpApi;

    /**
     * 查询用户维度计数.
     *
     * @param userId 用户 ID
     * @return 计数数据；调用失败返回 {@code null}
     */
    public FindUserCountRspDTO findUserCountById(Long userId) {
        FindUserCountReqDTO request = FindUserCountReqDTO.builder().userId(userId).build();
        Response<FindUserCountRspDTO> response = countHttpApi.findUserCount(request);
        if (Objects.isNull(response) || !response.isSuccess()) {
            log.error("==> 调用计数服务查询用户计数失败, userId: {}, response: {}", userId, response);
            return null;
        }
        return response.getData();
    }
}
