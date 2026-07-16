package com.hanserwei.comment.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 评论发布 MQ 消息体.
 *
 * @author hanserwei
 * @date 2026/07/14
 * @since 0.0.1
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class PublishCommentMqDTO {

    /** 评论 ID（发送前由 CoSId 预生成，用于幂等） */
    private Long commentId;
    /** 关联的笔记 ID */
    private Long noteId;
    /** 评论正文 UUID；发 MQ 前生成，保证消费重试写 ScyllaDB 使用同一主键 */
    private String contentUuid;
    /** 评论正文 */
    private String content;
    /** 评论附加图片 URL */
    private String imageUrl;
    /** 回复的评论 ID（0=直接对笔记评论） */
    private Long replyCommentId;
    /** 发布时间 */
    private LocalDateTime createTime;
    /** 发布者 ID */
    private Long creatorId;
}
