package com.hanserwei.comment.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;

/**
 * 评论点赞操作类型.
 *
 * @author hanserwei
 * @date 2026/07/15
 * @since 0.0.1
 */
@Getter
@AllArgsConstructor
public enum CommentLikeUnlikeTypeEnum {
    UNLIKE(0),
    LIKE(1);

    private final int code;

    public static CommentLikeUnlikeTypeEnum valueOf(Integer code) {
        return Arrays.stream(values()).filter(item -> item.code == (code == null ? -1 : code)).findFirst().orElse(null);
    }
}
