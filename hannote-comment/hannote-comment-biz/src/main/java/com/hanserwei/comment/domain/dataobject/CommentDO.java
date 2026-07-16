package com.hanserwei.comment.domain.dataobject;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 评论表数据对象（对应 t_comment）.
 *
 * <p>主键 {@code id} 由 CoSId 预生成后写入，非库自增，故 {@code IdType.INPUT}。
 *
 * @author hanserwei
 * @date 2026/07/14
 * @since 0.0.1
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@TableName("t_comment")
public class CommentDO {

    /** 评论 ID（CoSId 预生成） */
    @TableId(type = IdType.INPUT)
    private Long id;
    /** 关联的笔记 ID */
    private Long noteId;
    /** 发布者用户 ID */
    private Long userId;
    /** 评论正文 UUID（关联 ScyllaDB comment_content，正文不落本表） */
    private String contentUuid;
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
