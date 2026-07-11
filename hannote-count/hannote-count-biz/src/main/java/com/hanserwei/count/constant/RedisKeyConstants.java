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

    /**
     * 用户维度计数 Key 前缀（Hash 结构，field 为各计数维度）。
     */
    private static final String COUNT_USER_KEY_PREFIX = COUNT_KEY_PREFIX + "count:user:";

    /**
     * 笔记维度计数 Key 前缀（Hash 结构，field 为各计数维度）。
     */
    private static final String COUNT_NOTE_KEY_PREFIX = COUNT_KEY_PREFIX + "count:note:";

    /** Hash Field：粉丝总数 */
    public static final String FIELD_FANS_TOTAL = "fansTotal";

    /** Hash Field：关注总数 */
    public static final String FIELD_FOLLOWING_TOTAL = "followingTotal";

    /** Hash Field：点赞总数（笔记维度=被点赞数，用户维度=获赞数） */
    public static final String FIELD_LIKE_TOTAL = "likeTotal";

    /** Hash Field：收藏总数（笔记维度=被收藏数，用户维度=获藏数） */
    public static final String FIELD_COLLECT_TOTAL = "collectTotal";

    /** Hash Field：发布笔记总数（用户维度） */
    public static final String FIELD_NOTE_TOTAL = "noteTotal";

    /**
     * 构建用户维度计数 Key。
     *
     * @param userId 用户 ID
     * @return {@code hannote:count:count:user:{userId}}
     */
    public static String buildCountUserKey(Long userId) {
        return COUNT_USER_KEY_PREFIX + userId;
    }

    /**
     * 构建笔记维度计数 Key。
     *
     * @param noteId 笔记 ID
     * @return {@code hannote:count:count:note:{noteId}}
     */
    public static String buildCountNoteKey(Long noteId) {
        return COUNT_NOTE_KEY_PREFIX + noteId;
    }
}

