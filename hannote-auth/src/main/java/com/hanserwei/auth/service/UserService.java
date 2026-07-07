package com.hanserwei.auth.service;

import com.hanserwei.auth.model.vo.user.UpdatePasswordReqVO;
import com.hanserwei.auth.model.vo.user.UserLoginReqVO;
import com.hanserwei.framework.common.response.Response;

/**
 * 用户业务接口.
 *
 * @author hanserwei
 * @date 2026/07/07
 * @since 0.0.1
 */
public interface UserService {

    /**
     * 登录（新用户自动注册）.
     *
     * @param reqVO 登录请求入参
     * @return 登录成功返回 JWT 字符串
     */
    Response<?> loginAndRegister(UserLoginReqVO reqVO);

    /**
     * 修改密码.
     *
     * <p>从当前登录上下文获取用户 ID，将新密码 BCrypt 加密后更新到数据库。
     *
     * @param reqVO 修改密码请求入参
     * @return 操作结果
     */
    Response<?> updatePassword(UpdatePasswordReqVO reqVO);

    /**
     * 退出登录.
     *
     * <p>无状态 JWT 无法直接失效，将当前令牌加入 Redis 黑名单，
     * 过期时间对齐令牌剩余有效期，后续携带同一令牌的请求将被拒绝。
     *
     * @param token 当前请求携带的 JWT（不含 {@code Bearer } 前缀）
     * @return 操作结果
     */
    Response<?> logout(String token);
}
