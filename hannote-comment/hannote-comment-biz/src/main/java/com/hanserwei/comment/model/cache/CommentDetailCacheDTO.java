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

    /** 评论 ID */
    private Long commentId;
    /** 关联的笔记 ID */
    private Long noteId;
    /** 发布者用户 ID */
    private Long userId;
    /** 发布者头像 */
    private String avatar;
    /** 发布者昵称 */
    private String nickname;
    /** 评论正文（已从 ScyllaDB 回填） */
    private String content;
    /** 评论附加图片 URL */
    private String imageUrl;
    /** 级别：1 一级 / 2 二级 */
    private Integer level;
    /** 父 ID（对笔记评论=笔记 ID；二级评论=一级评论 ID） */
    private Long parentId;
    /** 回复的评论 ID（0=直接对笔记评论） */
    private Long replyCommentId;
    /** 回复的用户 ID */
    private Long replyUserId;
    /** 创建时间 */
    private LocalDateTime createTime;
}
