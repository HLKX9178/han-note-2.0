package com.hanserwei.auth.constant;

/**
 * Redis Key 全局常量.
 *
 * <p>统一管理业务中所有 Redis Key 的前缀与构建方法，避免硬编码分散导致 Key 命名冲突。
 * 所有 Key 以 {@code hannote:} 为命名空间前缀，便于多项目共享同一 Redis 实例时隔离。
 *
 * @author hanserwei
 * @date 2026/07/07
 * @since 0.0.1
 */
public final class RedisKeyConstants {

    private RedisKeyConstants() {
    }

    /**
     * 验证码 Key 前缀
     */
    public static final String VERIFICATION_CODE_KEY_PREFIX = "hannote:verification_code:";

    /**
     * ID 生成器 Key
     */
    public static final String HANNOTE_ID_GENERATOR = "hannote:id_generator";

    /**
     * 用户角色 Key 前缀
     */
    public static final String USER_ROLES_KEY_PREFIX = "hannote:user:roles:";

    /**
     * 角色权限 Key 前缀
     */
    public static final String ROLE_PERMISSIONS_KEY_PREFIX = "hannote:role:permissions:";

    /**
     * 构建验证码 Key
     *
     * @param phone 手机号
     * @return hannote:verification_code:{phone}
     */
    public static String buildVerificationCodeKey(String phone) {
        return VERIFICATION_CODE_KEY_PREFIX + phone;
    }

    /**
     * 构建用户角色 Key
     *
     * @param phone 手机号
     * @return hannote:user:roles:{phone}
     */
    public static String buildUserRoleKey(String phone) {
        return USER_ROLES_KEY_PREFIX + phone;
    }

    /**
     * 构建角色权限 Key
     *
     * @param roleId 角色 ID
     * @return hannote:role:permissions:{roleId}
     */
    public static String buildRolePermissionsKey(Long roleId) {
        return ROLE_PERMISSIONS_KEY_PREFIX + roleId;
    }
}
