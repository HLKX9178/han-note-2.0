package com.hanserwei.search.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Objects;

/**
 * 笔记排序类型.
 *
 * <p>对应搜索笔记入参 {@code sort}：{@code null} 表示综合排序（自定义评分）。
 *
 * @author hanserwei
 * @date 2026/07/13
 * @since 0.0.1
 */
@Getter
@AllArgsConstructor
public enum NoteSortTypeEnum {

    /** 最新（按发布时间降序） */
    LATEST(0),
    /** 最多点赞 */
    MOST_LIKE(1),
    /** 最多评论 */
    MOST_COMMENT(2),
    /** 最多收藏 */
    MOST_COLLECT(3),
    ;

    private final Integer code;

    /**
     * 按 code 获取枚举，无匹配返回 {@code null}（表示综合排序）。
     */
    public static NoteSortTypeEnum valueOf(Integer code) {
        for (NoteSortTypeEnum value : values()) {
            if (Objects.equals(code, value.getCode())) {
                return value;
            }
        }
        return null;
    }
}
