package com.hanserwei.auth.constant;

/**
 * Redis Key 全局常量.
 *
 * <p>认证服务持有的 Redis Key：验证码、JWT 登出黑名单。
 * 用户 ID 生成器、用户角色、角色权限等 Key 已随数据层迁移到用户服务。
 * 所有 Key 以 {@code hannote:} 为命名空间前缀。
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
     * JWT 登出黑名单 Key 前缀
     */
    public static final String TOKEN_BLACKLIST_KEY_PREFIX = "hannote:token:blacklist:";

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
     * 构建 JWT 黑名单 Key.
     *
     * <p>无状态 JWT 无法主动失效，登出时将令牌写入黑名单，
     * 过期时间对齐令牌剩余有效期，鉴权时命中黑名单即视为无效。
     *
     * @param token JWT 字符串
     * @return hannote:token:blacklist:{token}
     */
    public static String buildTokenBlacklistKey(String token) {
        return TOKEN_BLACKLIST_KEY_PREFIX + token;
    }
}
