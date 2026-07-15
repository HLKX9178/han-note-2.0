package com.hanserwei.comment.model.bo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 评论业务对象（消费端清洗、落库用）.
 *
 * @author hanserwei
 * @date 2026/07/14
 * @since 0.0.1
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class CommentBO {
    private Long id;
    private Long noteId;
    private Long userId;
    private String contentUuid;
    /** 评论正文（不落 t_comment，供 RPC 写 ScyllaDB） */
    private String content;
    private Boolean isContentEmpty;
    private String imageUrl;
    private Integer level;
    private Long replyTotal;
    private Long likeTotal;
    private Long firstReplyCommentId;
    private BigDecimal heat;
    private Long parentId;
    private Long replyCommentId;
    private Long replyUserId;
    private Boolean isTop;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
