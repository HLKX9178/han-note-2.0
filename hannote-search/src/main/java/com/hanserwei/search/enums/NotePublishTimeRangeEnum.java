package com.hanserwei.search.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Objects;

/**
 * 笔记发布时间范围.
 *
 * <p>对应搜索笔记入参 {@code publishTimeRange}：{@code null} 表示不限。
 *
 * @author hanserwei
 * @date 2026/07/13
 * @since 0.0.1
 */
@Getter
@AllArgsConstructor
public enum NotePublishTimeRangeEnum {

    /** 一天内 */
    DAY(0),
    /** 一周内 */
    WEEK(1),
    /** 半年内 */
    HALF_YEAR(2),
    ;

    private final Integer code;

    /**
     * 按 code 获取枚举，无匹配返回 {@code null}（表示不限时间）。
     */
    public static NotePublishTimeRangeEnum valueOf(Integer code) {
        for (NotePublishTimeRangeEnum value : values()) {
            if (Objects.equals(code, value.getCode())) {
                return value;
            }
        }
        return null;
    }
}
