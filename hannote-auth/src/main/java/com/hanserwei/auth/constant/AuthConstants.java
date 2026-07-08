package com.hanserwei.auth.constant;

/**
 * 认证服务常量.
 *
 * <p>认证服务在新用户注册后签发 JWT 时需要默认角色标识；角色数据的属主是用户服务，
 * 这里仅保留 JWT 声明所需的默认角色 key。
 *
 * @author hanserwei
 * @date 2026/07/08
 * @since 0.0.1
 */
public final class AuthConstants {

    private AuthConstants() {
    }

    /** 普通用户角色 Key（与 t_role.role_key 对应） */
    public static final String COMMON_USER_ROLE_KEY = "common_user";
}
