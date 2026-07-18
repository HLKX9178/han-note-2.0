package com.hanserwei.note.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Objects;

/**
 * Roaring Bitmap 校验并清位的 Lua 执行结果（取消点赞 / 取消收藏共用）.
 *
 * <p>对应 {@code rbitmap_check_and_remove.lua}，返回 -1/0/1。
 *
 * @author hanserwei
 * @date 2026/07/17
 * @since 0.0.1
 */
@Getter
@AllArgsConstructor
public enum NoteRBitmapCheckResultEnum {

    /** Roaring Bitmap 不存在 */
    NOT_EXIST(-1L),
    /** 原已置位（本次已清 0） */
    MARKED(1L),
    /** 未置位（精确判断） */
    NOT_MARKED(0L);

    private final Long code;

    /**
     * 根据 code 反查枚举.
     *
     * @param code Lua 返回值
     * @return 对应枚举；无匹配返回 {@code null}
     */
    public static NoteRBitmapCheckResultEnum valueOf(Long code) {
        for (NoteRBitmapCheckResultEnum e : values()) {
            if (Objects.equals(code, e.getCode())) {
                return e;
            }
        }
        return null;
    }
}
