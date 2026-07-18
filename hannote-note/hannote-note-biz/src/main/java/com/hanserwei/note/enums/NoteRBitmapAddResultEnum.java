package com.hanserwei.note.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Objects;

/**
 * Roaring Bitmap check-and-add / ZSET check-and-update 的 Lua 执行结果.
 *
 * <p>点赞与收藏的「加入」路径语义一致，故共用本枚举：
 * {@code rbitmap_check_and_add.lua} 返回 -1/1/0，{@code zset_check_and_update.lua} 返回 -1/0。
 *
 * @author hanserwei
 * @date 2026/07/17
 * @since 0.0.1
 */
@Getter
@AllArgsConstructor
public enum NoteRBitmapAddResultEnum {

    /** Roaring Bitmap / ZSET 不存在 */
    NOT_EXIST(-1L),
    /** 已置位（Roaring 精确，无需二次校验） */
    ALREADY(1L),
    /** 置位 / 更新成功 */
    SUCCESS(0L);

    private final Long code;

    /**
     * 根据 code 反查枚举.
     *
     * @param code Lua 返回值
     * @return 对应枚举；无匹配返回 {@code null}
     */
    public static NoteRBitmapAddResultEnum valueOf(Long code) {
        for (NoteRBitmapAddResultEnum e : values()) {
            if (Objects.equals(code, e.getCode())) {
                return e;
            }
        }
        return null;
    }
}
