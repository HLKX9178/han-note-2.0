package com.hanserwei.framework.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 逻辑删除枚举.
 *
 * <p>与 MyBatis-Plus 的 {@code @TableLogic} 配合，
 * 表示记录的逻辑删除状态（true：已删除，false：未删除）。
 *
 * @author hanserwei
 * @date 2026/07/07
 * @since 0.0.1
 */
@Getter
@AllArgsConstructor
public enum DeletedEnum {

    /** 已删除 */
    YES(true),
    /** 未删除 */
    NO(false);

    private final Boolean value;
}
