package com.hanserwei.count.service;

import com.hanserwei.count.api.dto.req.FindUserCountReqDTO;
import com.hanserwei.count.api.dto.resp.FindUserCountRspDTO;
import com.hanserwei.framework.common.response.Response;

/**
 * 用户维度计数查询业务.
 *
 * @author hanserwei
 * @date 2026/07/17
 * @since 0.0.1
 */
public interface UserCountQueryService {

    /**
     * 查询用户维度计数.
     *
     * @param request 查询入参
     * @return 用户计数
     */
    Response<FindUserCountRspDTO> findUserCountData(FindUserCountReqDTO request);
}
