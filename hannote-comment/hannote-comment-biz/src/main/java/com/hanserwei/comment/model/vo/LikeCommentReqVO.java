package com.hanserwei.comment.model.vo;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 评论点赞/取消点赞请求.
 *
 * @author hanserwei
 * @date 2026/07/15
 * @since 0.0.1
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LikeCommentReqVO {

    /** 目标评论 ID */
    @NotNull(message = "评论 ID 不能为空")
    private Long commentId;
}
