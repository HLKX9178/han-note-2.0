package com.hanserwei.dataalign.constant;

/**
 * Redis Key 全局常量.
 *
 * <p>分两部分：
 * <ul>
 *   <li><b>布隆过滤器 Key</b>（本服务自有，命名空间 {@code hannote:dataAlign:}）：
 *       日增量落库判重用，按日创建、约 20 小时过期；</li>
 *   <li><b>计数缓存 Key/Field 镜像</b>（<strong>必须与 {@code hannote-count} 的
 *       {@code RedisKeyConstants} 逐字节一致</strong>）：分片对齐任务回写计数缓存时使用，
 *       写错前缀/字段会写到错误的 Key。</li>
 * </ul>
 *
 * @author hanserwei
 * @date 2026/07/11
 * @since 0.0.1
 */
public final class RedisKeyConstants {

    private RedisKeyConstants() {
    }

    // ======================= 一、布隆过滤器 Key（本服务自有） =======================

    /** 布隆：日增量-笔记点赞数变更（笔记 ID 维度）前缀 */
    private static final String BLOOM_NOTE_LIKE_NOTE_ID_PREFIX = "hannote:dataAlign:note:like:noteIds:";

    /** 布隆：日增量-笔记点赞数变更（发布者 ID 维度）前缀 */
    private static final String BLOOM_NOTE_LIKE_USER_ID_PREFIX = "hannote:dataAlign:note:like:userIds:";

    /** 布隆：日增量-笔记收藏数变更（笔记 ID 维度）前缀 */
    private static final String BLOOM_NOTE_COLLECT_NOTE_ID_PREFIX = "hannote:dataAlign:note:collect:noteIds:";

    /** 布隆：日增量-笔记收藏数变更（发布者 ID 维度）前缀 */
    private static final String BLOOM_NOTE_COLLECT_USER_ID_PREFIX = "hannote:dataAlign:note:collect:userIds:";

    /** 布隆：日增量-用户发布笔记数变更（用户 ID 维度）前缀 */
    private static final String BLOOM_NOTE_OPERATE_USER_ID_PREFIX = "hannote:dataAlign:user:note:operate:";

    /** 布隆：日增量-用户关注数变更（源用户 ID 维度）前缀 */
    private static final String BLOOM_USER_FOLLOW_PREFIX = "hannote:dataAlign:user:follow:";

    /** 布隆：日增量-用户粉丝数变更（目标用户 ID 维度）前缀 */
    private static final String BLOOM_USER_FANS_PREFIX = "hannote:dataAlign:user:fans:";

    public static String buildBloomNoteLikeNoteIdKey(String date) {
        return BLOOM_NOTE_LIKE_NOTE_ID_PREFIX + date;
    }

    public static String buildBloomNoteLikeUserIdKey(String date) {
        return BLOOM_NOTE_LIKE_USER_ID_PREFIX + date;
    }

    public static String buildBloomNoteCollectNoteIdKey(String date) {
        return BLOOM_NOTE_COLLECT_NOTE_ID_PREFIX + date;
    }

    public static String buildBloomNoteCollectUserIdKey(String date) {
        return BLOOM_NOTE_COLLECT_USER_ID_PREFIX + date;
    }

    public static String buildBloomNoteOperateUserIdKey(String date) {
        return BLOOM_NOTE_OPERATE_USER_ID_PREFIX + date;
    }

    public static String buildBloomUserFollowKey(String date) {
        return BLOOM_USER_FOLLOW_PREFIX + date;
    }

    public static String buildBloomUserFansKey(String date) {
        return BLOOM_USER_FANS_PREFIX + date;
    }

    // ============ 二、计数缓存 Key/Field 镜像（须与 hannote-count RedisKeyConstants 一致） ============

    /** 计数服务 Redis Key 统一前缀（镜像 count 服务 {@code COUNT_KEY_PREFIX}） */
    private static final String COUNT_KEY_PREFIX = "hannote:count:";

    /** 用户维度计数 Key 前缀（Hash，镜像 count 服务） */
    private static final String COUNT_USER_KEY_PREFIX = COUNT_KEY_PREFIX + "count:user:";

    /** 笔记维度计数 Key 前缀（Hash，镜像 count 服务） */
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
     * 构建用户维度计数 Key（镜像 count 服务）。
     *
     * @param userId 用户 ID
     * @return {@code hannote:count:count:user:{userId}}
     */
    public static String buildCountUserKey(Long userId) {
        return COUNT_USER_KEY_PREFIX + userId;
    }

    /**
     * 构建笔记维度计数 Key（镜像 count 服务）。
     *
     * @param noteId 笔记 ID
     * @return {@code hannote:count:count:note:{noteId}}
     */
    public static String buildCountNoteKey(Long noteId) {
        return COUNT_NOTE_KEY_PREFIX + noteId;
    }
}
