package com.hanserwei.comment.model.cache;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 评论动态计数缓存.
 *
 * @author hanserwei
 * @date 2026/07/15
 * @since 0.0.1
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CommentCountCacheDTO {

    /** 评论 ID */
    private Long commentId;
    /** 被点赞次数 */
    private Long likeTotal;
    /** 被回复次数（仅一级评论） */
    private Long replyTotal;
    /** 最早回复评论 ID（仅一级评论，无则 0） */
    private Long firstReplyCommentId;
}
