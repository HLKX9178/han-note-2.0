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

    public static final String FIELD_LIKE_TOTAL = "likeTotal";
    public static final String FIELD_REPLY_TOTAL = "replyTotal";
    public static final String FIELD_FIRST_REPLY_COMMENT_ID = "firstReplyCommentId";
    public static final String FIELD_ROOT_TOTAL = "rootTotal";

    public static String buildRootListKey(Long noteId) {
        return PREFIX + "list:root:" + noteId;
    }

    public static String buildChildListKey(Long rootId) {
        return PREFIX + "list:child:" + rootId;
    }

    public static String buildDetailKey(Long commentId) {
        return PREFIX + "detail:" + commentId;
    }

    public static String buildCountKey(Long commentId) {
        return PREFIX + "count:" + commentId;
    }

    public static String buildNoteCountKey(Long noteId) {
        return PREFIX + "count:note:" + noteId;
    }

    public static String buildEmptyRootKey(Long noteId) {
        return PREFIX + "empty:root:" + noteId;
    }

    public static String buildEmptyChildKey(Long rootId) {
        return PREFIX + "empty:child:" + rootId;
    }

    public static String buildLikeBloomKey(Long userId) {
        return PREFIX + "bloom:like:" + userId;
    }
}
