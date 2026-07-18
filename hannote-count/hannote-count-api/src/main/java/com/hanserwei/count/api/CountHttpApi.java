package com.hanserwei.count.api;

import com.hanserwei.count.api.dto.req.FindNoteCountReqDTO;
import com.hanserwei.count.api.dto.req.FindNoteCountsByIdsReqDTO;
import com.hanserwei.count.api.dto.req.FindUserCountReqDTO;
import com.hanserwei.count.api.dto.resp.FindNoteCountRspDTO;
import com.hanserwei.count.api.dto.resp.FindUserCountRspDTO;
import com.hanserwei.framework.common.response.Response;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.service.annotation.HttpExchange;
import org.springframework.web.service.annotation.PostExchange;

import java.util.List;

/**
 * 计数服务内网 HTTP Interface 契约.
 *
 * @author hanserwei
 * @date 2026/07/15
 * @since 0.0.1
 */
@HttpExchange
public interface CountHttpApi {

    /** 计数服务内网接口统一前缀 */
    String PREFIX = "/count";

    /**
     * 查询笔记维度计数.
     *
     * @param request 查询入参
     * @return 笔记计数
     */
    @PostExchange(PREFIX + "/note/findById")
    Response<FindNoteCountRspDTO> findNoteCountById(@RequestBody FindNoteCountReqDTO request);

    /**
     * 批量查询笔记维度计数.
     *
     * @param request 查询入参（笔记 ID 集合）
     * @return 各笔记的计数集合
     */
    @PostExchange(PREFIX + "/notes/data")
    Response<List<FindNoteCountRspDTO>> findNotesCountData(@RequestBody FindNoteCountsByIdsReqDTO request);

    /**
     * 查询用户维度计数.
     *
     * @param request 查询入参（用户 ID）
     * @return 用户计数
     */
    @PostExchange(PREFIX + "/user/data")
    Response<FindUserCountRspDTO> findUserCount(@RequestBody FindUserCountReqDTO request);
}
