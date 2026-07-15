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

    private Long commentId;
    private Long userId;
    private String avatar;
    private String nickname;
    private String content;
    private String imageUrl;
    private String createTime;
    private Long likeTotal;
    private String replyUserName;
    private Long replyUserId;
}
