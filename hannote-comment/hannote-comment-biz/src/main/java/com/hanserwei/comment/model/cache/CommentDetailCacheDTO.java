package com.hanserwei.comment.model.cache;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 评论静态详情缓存，不包含动态计数字段.
 *
 * @author hanserwei
 * @date 2026/07/15
 * @since 0.0.1
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CommentDetailCacheDTO {

    private Long commentId;
    private Long noteId;
    private Long userId;
    private String avatar;
    private String nickname;
    private String content;
    private String imageUrl;
    private Integer level;
    private Long parentId;
    private Long replyCommentId;
    private Long replyUserId;
    private LocalDateTime createTime;
}
