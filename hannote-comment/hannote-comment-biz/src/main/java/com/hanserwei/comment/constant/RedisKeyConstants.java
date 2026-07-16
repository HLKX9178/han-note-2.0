package com.hanserwei.comment.constant;

/**
 * 评论服务 Redis Key 常量.
 *
 * @author hanserwei
 * @date 2026/07/15
 * @since 0.0.1
 */
public final class RedisKeyConstants {

    private RedisKeyConstants() {
    }

    private static final String PREFIX = "hannote:comment:";

    /**
     * 评论点赞总数.
     */
    public static final String FIELD_LIKE_TOTAL = "likeTotal";
    /**
     * 评论回复总数.
     */
    public static final String FIELD_REPLY_TOTAL = "replyTotal";
    /**
     * 评论第一条回复的ID.
     */
    public static final String FIELD_FIRST_REPLY_COMMENT_ID = "firstReplyCommentId";
    /**
     * 评论根评论总数.
     */
    public static final String FIELD_ROOT_TOTAL = "rootTotal";

    /**
     * 根据笔记ID构建根评论列表的Redis Key.
     *
      * @param noteId 笔记ID
      * @return Redis Key
     */
    public static String buildRootListKey(Long noteId) {
        return PREFIX + "list:root:" + noteId;
    }

    /**
     * 根据根评论ID构建子评论列表的Redis Key.
     *
     * @param rootId 根评论ID
     * @return Redis Key
     */
    public static String buildChildListKey(Long rootId) {
        return PREFIX + "list:child:" + rootId;
    }

    /**
     * 根据评论ID构建评论详情的Redis Key.
     *
     * @param commentId 评论ID
     * @return Redis Key
     */
    public static String buildDetailKey(Long commentId) {
        return PREFIX + "detail:" + commentId;
    }

    /**
     * 根据评论ID构建评论统计的Redis Key.
     *
     * @param commentId 评论ID
     * @return Redis Key
     */
    public static String buildCountKey(Long commentId) {
        return PREFIX + "count:" + commentId;
    }

    /**
     * 根据笔记ID构建笔记评论统计的Redis Key.
     *
     * @param noteId 笔记ID
     * @return Redis Key
     */
    public static String buildNoteCountKey(Long noteId) {
        return PREFIX + "count:note:" + noteId;
    }

    /**
     * 根据笔记ID构建根评论为空的Redis Key.
     *
     * @param noteId 笔记ID
     * @return Redis Key
     */
    public static String buildEmptyRootKey(Long noteId) {
        return PREFIX + "empty:root:" + noteId;
    }

    /**
     * 根据根评论ID构建子评论为空的Redis Key.
     *
     * @param rootId 根评论ID
     * @return Redis Key
     */
    public static String buildEmptyChildKey(Long rootId) {
        return PREFIX + "empty:child:" + rootId;
    }

    /**
     * 根据用户ID构建点赞布隆过滤器的Redis Key.
     *
     * @param userId 用户ID
     * @return Redis Key
     */
    public static String buildLikeBloomKey(Long userId) {
        return PREFIX + "bloom:like:" + userId;
    }
}
