package com.hanserwei.count.controller;

import com.hanserwei.count.api.dto.req.FindNoteCountReqDTO;
import com.hanserwei.count.api.dto.req.FindUserCountReqDTO;
import com.hanserwei.count.api.dto.resp.FindNoteCountRspDTO;
import com.hanserwei.count.api.dto.resp.FindUserCountRspDTO;
import com.hanserwei.count.service.CountQueryService;
import com.hanserwei.count.service.UserCountQueryService;
import com.hanserwei.framework.common.response.Response;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 计数服务内网查询控制器.
 *
 * @author hanserwei
 * @date 2026/07/15
 * @since 0.0.1
 */
@RestController
@RequestMapping("/count")
@RequiredArgsConstructor
public class CountController {

    private final CountQueryService countQueryService;
    private final UserCountQueryService userCountQueryService;

    /**
     * 查询笔记维度计数.
     */
    @PostMapping("/note/findById")
    public Response<FindNoteCountRspDTO> findNoteCountById(
            @Validated @RequestBody FindNoteCountReqDTO request) {
        return countQueryService.findNoteCountById(request);
    }

    /**
     * 查询用户维度计数.
     *
     * @param request 查询入参（用户 ID）
     * @return 用户计数
     */
    @PostMapping("/user/data")
    public Response<FindUserCountRspDTO> findUserCountData(
            @Validated @RequestBody FindUserCountReqDTO request) {
        return userCountQueryService.findUserCountData(request);
    }
}
