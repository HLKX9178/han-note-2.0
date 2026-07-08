package com.hanserwei.user.constant;

/**
 * 角色全局常量.
 *
 * <p>定义系统中所有角色的固定标识，供自动注册分配默认角色等流程引用。
 *
 * @author hanserwei
 * @date 2026/07/08
 * @since 0.0.1
 */
public final class RoleConstants {

    private RoleConstants() {
    }

    /** 普通用户角色 ID（与 t_role 初始数据对应） */
    public static final Long COMMON_USER_ROLE_ID = 1L;

    /** 普通用户角色 Key（与 t_role.role_key 对应） */
    public static final String COMMON_USER_ROLE_KEY = "common_user";
}
