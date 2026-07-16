package com.hanserwei.comment.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 评论点赞/取消点赞顺序消息.
 *
 * @author hanserwei
 * @date 2026/07/15
 * @since 0.0.1
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LikeUnlikeCommentMqDTO {

    /** 操作用户 ID */
    private Long userId;
    /** 被点赞/取消点赞的评论 ID */
    private Long commentId;
    /** 操作类型：0 取消点赞 / 1 点赞（见 CommentLikeUnlikeTypeEnum） */
    private Integer type;
    /** 操作时间 */
    private LocalDateTime createTime;
}
