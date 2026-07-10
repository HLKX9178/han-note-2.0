package com.hanserwei.relation.constant;

/**
 * Redis Key 全局常量.
 *
 * <p>用户关系服务持有的 Redis Key。所有 Key 以 {@code hannote:} 为命名空间前缀，
 * 关系相关进一步收束到 {@code hannote:relation:} 子前缀。本期（��
 * 仅预留前缀常量，后续关注/粉丝列表缓存、计数缓存等场景在此追加。
 *
 * @author hanserwei
 * @date 2026/07/10
 * @since 0.0.1
 */
public final class RedisKeyConstants {

    private RedisKeyConstants() {
    }

    /**
     * 用户关系服务 Redis Key 统一前缀。
     */
    public static final String RELATION_KEY_PREFIX = "hannote:relation:";
}
