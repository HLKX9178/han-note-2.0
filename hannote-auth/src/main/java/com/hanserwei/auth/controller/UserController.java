package com.hanserwei.auth.controller;

import com.hanserwei.auth.model.vo.user.UserLoginReqVO;
import com.hanserwei.auth.service.UserService;
import com.hanserwei.framework.biz.operationlog.aspect.ApiOperationLog;
import com.hanserwei.framework.common.response.Response;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * 用户控制器.
 *
 * <p>对外暴露 {@code POST /user/login} 接口，
 * 提供手机号验证码登录（新用户自动注册）与账号密码登录（阶段一未启用）能力。
 * 登录成功后返回 JWT，客户端需在后续请求中以 {@code Authorization: Bearer xxx} 形式携带。
 *
 * @author hanserwei
 * @date 2026/07/07
 * @since 0.0.1
 */
@RestController
@Slf4j
public class UserController {

    @Resource
    private UserService userService;

    /**
     * 用户登录（新用户自动注册）.
     *
     * @param reqVO 登录请求入参
     * @return 成功时 {@code data} 为 JWT 字符串
     */
    @PostMapping("/user/login")
    @ApiOperationLog(description = "用户登录（新用户自动注册）")
    public Response<?> login(@Validated @RequestBody UserLoginReqVO reqVO) {
        return userService.loginAndRegister(reqVO);
    }
}
