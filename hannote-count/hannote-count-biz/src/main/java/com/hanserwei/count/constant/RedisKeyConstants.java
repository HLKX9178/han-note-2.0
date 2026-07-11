package com.hanserwei.count.constant;

/**
 * Redis Key 全局常量.
 *
 * <p>计数服务持有的 Redis Key。所有 Key 以 {@code hannote:} 为命名空间前缀，
 * 计数相关进一步收束到 {@code hannote:count:} 子前缀。本期（��
 * 仅预留前缀常量，后续笔记计数、用户计数缓存等场景在此追加。
 *
 * @author hanserwei
 * @date 2026/07/11
 * @since 0.0.1
 */
public final class RedisKeyConstants {

    private RedisKeyConstants() {
    }

    /**
     * 计数服务 Redis Key 统一前缀。
     */
    public static final String COUNT_KEY_PREFIX = "hannote:count:";
}
