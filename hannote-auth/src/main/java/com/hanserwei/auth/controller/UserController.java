package com.hanserwei.auth.controller;

import com.hanserwei.auth.model.vo.user.UpdatePasswordReqVO;
import com.hanserwei.auth.model.vo.user.UserLoginReqVO;
import com.hanserwei.auth.service.UserService;
import com.hanserwei.framework.biz.operationlog.aspect.ApiOperationLog;
import com.hanserwei.framework.common.response.Response;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
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

    /**
     * 修改密码.
     *
     * <p>需登录后调用，从 JWT 上下文获取当前用户 ID。
     *
     * @param reqVO 修改密码请求入参
     * @return 操作结果
     */
    @PostMapping("/user/password/update")
    @ApiOperationLog(description = "修改密码")
    public Response<?> updatePassword(@Validated @RequestBody UpdatePasswordReqVO reqVO) {
        return userService.updatePassword(reqVO);
    }

    /**
     * 退出登录.
     *
     * <p>将当前 {@code Authorization} 请求头携带的 JWT 加入黑名单使其失效。
     *
     * @param authorization {@code Authorization} 请求头（形如 {@code Bearer <jwt>}）
     * @return 操作结果
     */
    @PostMapping("/user/logout")
    @ApiOperationLog(description = "退出登录")
    public Response<?> logout(@RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization) {
        String token = (authorization != null && authorization.startsWith("Bearer "))
                ? authorization.substring(7)
                : null;
        return userService.logout(token);
    }
}
