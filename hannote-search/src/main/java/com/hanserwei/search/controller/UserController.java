package com.hanserwei.search.controller;

import com.hanserwei.framework.biz.operationlog.aspect.ApiOperationLog;
import com.hanserwei.framework.common.response.PageResponse;
import com.hanserwei.search.model.vo.SearchUserReqVO;
import com.hanserwei.search.model.vo.SearchUserRspVO;
import com.hanserwei.search.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 用户搜索控制器.
 *
 * @author hanserwei
 * @date 2026/07/13
 * @since 0.0.1
 */
@RestController
@RequestMapping("/search")
@Slf4j
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @PostMapping("/user")
    @ApiOperationLog(description = "搜索用户")
    public PageResponse<SearchUserRspVO> searchUser(@RequestBody @Valid SearchUserReqVO searchUserReqVO) {
        return userService.searchUser(searchUserReqVO);
    }
}
