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
    /** 评论 ID（CoSId 预生成） */
    private Long id;
    /** 关联的笔记 ID */
    private Long noteId;
    /** 发布者用户 ID */
    private Long userId;
    /** 评论正文 UUID（关联 ScyllaDB comment_content） */
    private String contentUuid;
    /** 评论正文（不落 t_comment，供 RPC 写 ScyllaDB） */
    private String content;
    /** 内容是否为空（仅图片评论时为 true） */
    private Boolean isContentEmpty;
    /** 评论附加图片 URL */
    private String imageUrl;
    /** 级别：1 一级 / 2 二级 */
    private Integer level;
    /** 被回复次数（仅一级评论维护） */
    private Long replyTotal;
    /** 被点赞次数 */
    private Long likeTotal;
    /** 最早回复评论 ID（仅一级评论，无则 0） */
    private Long firstReplyCommentId;
    /** 评论热度（点赞 70% + 回复 30%，仅一级评论） */
    private BigDecimal heat;
    /** 父 ID（对笔记评论=笔记 ID；二级评论=一级评论 ID） */
    private Long parentId;
    /** 回复的评论 ID（0=直接对笔记评论） */
    private Long replyCommentId;
    /** 回复的用户 ID */
    private Long replyUserId;
    /** 是否置顶 */
    private Boolean isTop;
    /** 创建时间 */
    private LocalDateTime createTime;
    /** 更新时间 */
    private LocalDateTime updateTime;
}
