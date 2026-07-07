package com.hanserwei.framework.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 通用状态枚举.
 *
 * <p>适用于角色、权限、用户等实体的"启用/禁用"状态字段。
 *
 * @author hanserwei
 * @date 2026/07/07
 * @since 0.0.1
 */
@Getter
@AllArgsConstructor
public enum StatusEnum {

    /** 启用 */
    ENABLE(0),
    /** 禁用 */
    DISABLED(1);

    private final Integer value;
}
