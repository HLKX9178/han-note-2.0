package com.hanserwei.count.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Objects;

/**
 * 笔记收藏 / 取消收藏操作类型（计数服务侧，与笔记服务镜像）.
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
     * 按 code 反查枚举。
     *
     * @param code 操作类型 code
     * @return 匹配的枚举；无匹配返回 {@code null}
     */
    public static CollectUnCollectNoteTypeEnum valueOf(Integer code) {
        for (CollectUnCollectNoteTypeEnum typeEnum : values()) {
            if (Objects.equals(code, typeEnum.getCode())) {
                return typeEnum;
            }
        }
        return null;
    }
}
