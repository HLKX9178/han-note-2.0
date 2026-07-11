package com.hanserwei.note.constant;

/**
 * Redis Key 全局常量.
 *
 * <p>笔记服务持有的 Redis Key。所有 Key 以 {@code hannote:} 为命名空间前缀，
 * 笔记相关进一步收束到 {@code hannote:note:} 子前缀。
 *
 * @author hanserwei
 * @date 2026/07/09
 * @since 0.0.1
 */
public final class RedisKeyConstants {

    private RedisKeyConstants() {
    }

    /**
     * 笔记详情 Key 前缀（预留，供笔记详情缓存使用）
     */
    public static final String NOTE_DETAIL_KEY_PREFIX = "hannote:note:detail:";

    /**
     * 构建笔记详情 Key
     *
     * @param noteId 笔记 ID
     * @return hannote:note:detail:{noteId}
     */
    public static String buildNoteDetailKey(Long noteId) {
        return NOTE_DETAIL_KEY_PREFIX + noteId;
    }

    /** 用户笔记点赞布隆过滤器 Key 前缀 */
    public static final String BLOOM_NOTE_LIKE_KEY_PREFIX = "hannote:note:bloom:like:";

    /** 用户笔记收藏布隆过滤器 Key 前缀 */
    public static final String BLOOM_NOTE_COLLECT_KEY_PREFIX = "hannote:note:bloom:collect:";

    /** 用户笔记点赞列表 ZSET Key 前缀 */
    public static final String ZSET_NOTE_LIKE_KEY_PREFIX = "hannote:note:zset:like:";

    /** 用户笔记收藏列表 ZSET Key 前缀 */
    public static final String ZSET_NOTE_COLLECT_KEY_PREFIX = "hannote:note:zset:collect:";

    /**
     * 构建用户笔记点赞布隆过滤器 Key
     *
     * @param userId 用户 ID
     * @return hannote:note:bloom:like:{userId}
     */
    public static String buildBloomNoteLikeKey(Long userId) {
        return BLOOM_NOTE_LIKE_KEY_PREFIX + userId;
    }

    /**
     * 构建用户笔记收藏布隆过滤器 Key
     *
     * @param userId 用户 ID
     * @return hannote:note:bloom:collect:{userId}
     */
    public static String buildBloomNoteCollectKey(Long userId) {
        return BLOOM_NOTE_COLLECT_KEY_PREFIX + userId;
    }

    /**
     * 构建用户笔记点赞列表 ZSET Key
     *
     * @param userId 用户 ID
     * @return hannote:note:zset:like:{userId}
     */
    public static String buildZSetNoteLikeKey(Long userId) {
        return ZSET_NOTE_LIKE_KEY_PREFIX + userId;
    }

    /**
     * 构建用户笔记收藏列表 ZSET Key
     *
     * @param userId 用户 ID
     * @return hannote:note:zset:collect:{userId}
     */
    public static String buildZSetNoteCollectKey(Long userId) {
        return ZSET_NOTE_COLLECT_KEY_PREFIX + userId;
    }
}
