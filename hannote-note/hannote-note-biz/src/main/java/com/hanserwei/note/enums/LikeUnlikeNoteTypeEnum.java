package com.hanserwei.note.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Objects;

/**
 * 笔记点赞 / 取消点赞操作类型.
 *
 * <p>其 code 同时用作 {@code t_note_like.status} 业务状态值（1 点赞 / 0 取消点赞）。
 *
 * @author hanserwei
 * @date 2026/07/11
 * @since 0.0.1
 */
@Getter
@AllArgsConstructor
public enum LikeUnlikeNoteTypeEnum {

    /** 点赞 */
    LIKE(1),
    /** 取消点赞 */
    UNLIKE(0);

    private final Integer code;

    /**
     * 根据 code 反查枚举.
     *
     * @param code 操作类型码
     * @return 对应枚举；无匹配返回 {@code null}
     */
    public static LikeUnlikeNoteTypeEnum valueOf(Integer code) {
        for (LikeUnlikeNoteTypeEnum e : values()) {
            if (Objects.equals(code, e.getCode())) {
                return e;
            }
        }
        return null;
    }
}
