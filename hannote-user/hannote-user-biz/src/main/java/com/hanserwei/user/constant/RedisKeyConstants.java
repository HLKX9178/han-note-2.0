package com.hanserwei.user.constant;

/**
 * Redis Key 全局常量.
 *
 * <p>用户服务持有的 Redis Key：全局 ID 生成器、用户角色缓存、角色权限缓存。
 * 所有 Key 以 {@code hannote:} 为命名空间前缀。
 *
 * @author hanserwei
 * @date 2026/07/08
 * @since 0.0.1
 */
public final class RedisKeyConstants {

    private RedisKeyConstants() {
    }

    /**
     * 用户角色 Key 前缀
     */
    public static final String USER_ROLES_KEY_PREFIX = "hannote:user:roles:";

    /**
     * 角色权限 Key 前缀
     */
    public static final String ROLE_PERMISSIONS_KEY_PREFIX = "hannote:role:permissions:";

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
