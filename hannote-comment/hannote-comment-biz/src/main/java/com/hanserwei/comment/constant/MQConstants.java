package com.hanserwei.comment.constant;

/**
 * 评论服务 MQ 常量.
 *
 * @author hanserwei
 * @date 2026/07/14
 * @since 0.0.1
 */
public interface MQConstants {

    /** Topic：评论发布 */
    String TOPIC_PUBLISH_COMMENT = "PublishCommentTopic";

    /** Topic：笔记评论总数变更 */
    String TOPIC_COMMENT_COUNT_CHANGED = "CommentCountChangedTopic";

    /** Topic：评论点赞/取消点赞 */
    String TOPIC_LIKE_UNLIKE_COMMENT = "LikeUnlikeCommentTopic";

    /** Topic：广播删除评论本地缓存 */
    String TOPIC_DELETE_COMMENT_LOCAL_CACHE = "DeleteCommentLocalCacheTopic";

    /** Topic：删除评论正文 */
    String TOPIC_DELETE_COMMENT_CONTENT = "DeleteCommentContentTopic";

    String TAG_LIKE = "Like";
    String TAG_UNLIKE = "Unlike";

    String GROUP_DELETE_LOCAL_CACHE = "hannote_comment_delete_local_cache_group";
    String GROUP_DELETE_CONTENT = "hannote_comment_delete_content_group";
}
