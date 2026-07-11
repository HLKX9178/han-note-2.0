package com.hanserwei.note.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Objects;

/**
 * 布隆过滤器存在性校验的 Lua 执行结果（取消点赞 / 取消收藏共用）.
 *
 * <p>对应 {@code bloom_exist.lua}，返回 -1/1/0。
 *
 * @author hanserwei
 * @date 2026/07/11
 * @since 0.0.1
 */
@Getter
@AllArgsConstructor
public enum NoteBloomCheckResultEnum {

    /** 布隆过滤器不存在 */
    NOT_EXIST(-1L),
    /** 已标记（可能误判） */
    MARKED(1L),
    /** 未标记（判断绝对正确） */
    NOT_MARKED(0L);

    private final Long code;

    /**
     * 根据 code 反查枚举.
     *
     * @param code Lua 返回值
     * @return 对应枚举；无匹配返回 {@code null}
     */
    public static NoteBloomCheckResultEnum valueOf(Long code) {
        for (NoteBloomCheckResultEnum e : values()) {
            if (Objects.equals(code, e.getCode())) {
                return e;
            }
        }
        return null;
    }
}
