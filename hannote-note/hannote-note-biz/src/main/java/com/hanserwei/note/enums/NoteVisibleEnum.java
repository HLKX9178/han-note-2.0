package com.hanserwei.note.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Objects;

/**
 * 笔记可见性.
 *
 * @author hanserwei
 * @date 2026/07/09
 * @since 0.0.1
 */
@Getter
@AllArgsConstructor
public enum NoteVisibleEnum {

    /** 公开，所有人可见 */
    PUBLIC(0),
    /** 仅自己可见 */
    PRIVATE(1);

    private final Integer code;

    /**
     * 根据可见性 code 获取对应的枚举
     *
     * @param code 可见性码
     * @return 对应枚举；无匹配返回 null
     */
    public static NoteVisibleEnum of(Integer code) {
        for (NoteVisibleEnum e : NoteVisibleEnum.values()) {
            if (Objects.equals(code, e.getCode())) {
                return e;
            }
        }
        return null;
    }
}
