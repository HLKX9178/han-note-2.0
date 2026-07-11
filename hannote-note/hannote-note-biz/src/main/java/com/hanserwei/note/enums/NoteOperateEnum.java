package com.hanserwei.note.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 笔记操作类型（发布 / 删除）.
 *
 * @author hanserwei
 * @date 2026/07/11
 * @since 0.0.1
 */
@Getter
@AllArgsConstructor
public enum NoteOperateEnum {

    /** 发布 */
    PUBLISH(1),
    /** 删除 */
    DELETE(0);

    private final Integer code;
}
