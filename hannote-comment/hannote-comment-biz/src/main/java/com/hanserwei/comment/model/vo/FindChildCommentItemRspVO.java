package com.hanserwei.comment.model.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 二级评论展示项.
 *
 * @author hanserwei
 * @date 2026/07/15
 * @since 0.0.1
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FindChildCommentItemRspVO {

    /** 评论 ID */
    private Long commentId;
    /** 发布者用户 ID */
    private Long userId;
    /** 发布者头像 */
    private String avatar;
    /** 发布者昵称 */
    private String nickname;
    /** 评论正文 */
    private String content;
    /** 评论附加图片 URL */
    private String imageUrl;
    /** 发布时间（已格式化展示） */
    private String createTime;
    /** 被点赞次数 */
    private Long likeTotal;
    /** 被回复用户昵称（回复他人评论时展示） */
    private String replyUserName;
    /** 被回复用户 ID */
    private Long replyUserId;
}
