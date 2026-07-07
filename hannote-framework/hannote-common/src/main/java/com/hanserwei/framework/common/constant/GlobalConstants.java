package com.hanserwei.framework.common.constant;

/**
 * 全局通用常量.
 *
 * <p>放置各模块通用的键名等常量，避免在网关、认证服务等多处重复定义。
 *
 * @author hanserwei
 * @date 2026/07/07
 * @since 0.0.1
 */
public interface GlobalConstants {

    /** 网关透传用户 ID 的请求头键名 */
    String USER_ID = "userId";
}
