package com.hanserwei.note.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Objects;

/**
 * 笔记状态.
 *
 * @author hanserwei
 * @date 2026/07/09
 * @since 0.0.1
 */
@Getter
@AllArgsConstructor
public enum NoteStatusEnum {

    /** 待审核 */
    BE_EXAMINE(0),
    /** 正常展示 */
    NORMAL(1),
    /** 被删除 */
    DELETED(2),
    /** 被下架 */
    DOWNED(3);

    private final Integer code;

    /**
     * 根据状态 code 获取对应的枚举
     *
     * @param code 状态码
     * @return 对应枚举；无匹配返回 null
     */
    public static NoteStatusEnum of(Integer code) {
        for (NoteStatusEnum e : NoteStatusEnum.values()) {
            if (Objects.equals(code, e.getCode())) {
                return e;
            }
        }
        return null;
    }
}
