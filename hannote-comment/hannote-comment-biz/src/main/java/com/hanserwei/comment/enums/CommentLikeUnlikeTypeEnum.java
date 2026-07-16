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
    /** 取消点赞 */
    UNLIKE(0),
    /** 点赞 */
    LIKE(1);

    /** 类型编码（持久化/传输用） */
    private final int code;

    /**
     * 按编码反查枚举.
     *
     * @param code 类型编码，可为 null
     * @return 匹配的枚举；无匹配或入参为 null 时返回 {@code null}
     */
    public static CommentLikeUnlikeTypeEnum valueOf(Integer code) {
        return Arrays.stream(values()).filter(item -> item.code == (code == null ? -1 : code)).findFirst().orElse(null);
    }
}
