package com.hanserwei.note.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Objects;

/**
 * 笔记类型.
 *
 * @author hanserwei
 * @date 2026/07/09
 * @since 0.0.1
 */
@Getter
@AllArgsConstructor
public enum NoteTypeEnum {

    /** 图文 */
    IMAGE_TEXT(0, "图文"),
    /** 视频 */
    VIDEO(1, "视频");

    private final Integer code;
    private final String description;

    /**
     * 类型是否有效
     *
     * @param code 类型码
     * @return 有效返回 true
     */
    public static boolean isValid(Integer code) {
        for (NoteTypeEnum e : NoteTypeEnum.values()) {
            if (Objects.equals(code, e.getCode())) {
                return true;
            }
        }
        return false;
    }

    /**
     * 根据类型 code 获取对应的枚举
     *
     * @param code 类型码
     * @return 对应枚举；无匹配返回 null
     */
    public static NoteTypeEnum of(Integer code) {
        for (NoteTypeEnum e : NoteTypeEnum.values()) {
            if (Objects.equals(code, e.getCode())) {
                return e;
            }
        }
        return null;
    }
}
