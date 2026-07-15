package com.hanserwei.comment.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 评论级别.
 *
 * @author hanserwei
 * @date 2026/07/14
 * @since 0.0.1
 */
@Getter
@AllArgsConstructor
public enum CommentLevelEnum {

    /** 一级评论 */
    ONE(1),
    /** 二级评论 */
    TWO(2),
    ;

    private final Integer code;
}
