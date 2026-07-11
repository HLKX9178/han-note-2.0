package com.hanserwei.note.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Objects;

/**
 * 笔记收藏 / 取消收藏操作类型.
 *
 * <p>其 code 同时用作 {@code t_note_collection.status} 业务状态值（1 收藏 / 0 取消收藏）。
 *
 * @author hanserwei
 * @date 2026/07/11
 * @since 0.0.1
 */
@Getter
@AllArgsConstructor
public enum CollectUnCollectNoteTypeEnum {

    /** 收藏 */
    COLLECT(1),
    /** 取消收藏 */
    UN_COLLECT(0);

    private final Integer code;

    /**
     * 根据 code 反查枚举.
     *
     * @param code 操作类型码
     * @return 对应枚举；无匹配返回 {@code null}
     */
    public static CollectUnCollectNoteTypeEnum valueOf(Integer code) {
        for (CollectUnCollectNoteTypeEnum e : values()) {
            if (Objects.equals(code, e.getCode())) {
                return e;
            }
        }
        return null;
    }
}
