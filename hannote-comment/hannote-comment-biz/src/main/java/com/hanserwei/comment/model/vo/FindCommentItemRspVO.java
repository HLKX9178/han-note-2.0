package com.hanserwei.comment.model.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * 一级评论展示项（也用于内嵌首条回复）.
 *
 * @author hanserwei
 * @date 2026/07/15
 * @since 0.0.1
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FindCommentItemRspVO {

    private Long commentId;
    private Long userId;
    private String avatar;
    private String nickname;
    private String content;
    private String imageUrl;
    private String createTime;
    private Long likeTotal;
    private Long childCommentTotal;
    private BigDecimal heat;
    private FindCommentItemRspVO firstReplyComment;
}
