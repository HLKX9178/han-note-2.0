package com.hanserwei.comment.model.vo;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 评论发布请求.
 *
 * @author hanserwei
 * @date 2026/07/14
 * @since 0.0.1
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class PublishCommentReqVO {

    /** 笔记 ID */
    @NotNull(message = "笔记 ID 不能为空")
    private Long noteId;

    /** 评论内容（与图片不可同时为空） */
    private String content;

    /** 评论图片链接 */
    private String imageUrl;

    /** 回复的评论 ID（为空=直接评论笔记） */
    private Long replyCommentId;
}
