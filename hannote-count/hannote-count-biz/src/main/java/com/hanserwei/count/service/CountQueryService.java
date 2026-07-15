package com.hanserwei.count.service;

import com.hanserwei.count.api.dto.req.FindNoteCountReqDTO;
import com.hanserwei.count.api.dto.resp.FindNoteCountRspDTO;
import com.hanserwei.framework.common.response.Response;

/**
 * 计数查询业务.
 *
 * @author hanserwei
 * @date 2026/07/15
 * @since 0.0.1
 */
public interface CountQueryService {

    /**
     * 查询笔记维度计数.
     *
     * @param request 查询入参
     * @return 笔记计数
     */
    Response<FindNoteCountRspDTO> findNoteCountById(FindNoteCountReqDTO request);
}
