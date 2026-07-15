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

    private Long commentId;
    private Long likeTotal;
    private Long replyTotal;
    private Long firstReplyCommentId;
}
