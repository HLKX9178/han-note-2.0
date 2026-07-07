package com.hanserwei.user.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Objects;

/**
 * 性别枚举.
 *
 * @author hanserwei
 * @date 2026/07/07
 * @since 0.0.1
 */
@Getter
@AllArgsConstructor
public enum SexEnum {

    WOMAN(0),
    MAN(1);

    private final Integer value;

    /**
     * 校验性别值是否合法.
     *
     * @param value 待校验值
     * @return 合法返回 {@code true}
     */
    public static boolean isValid(Integer value) {
        if (Objects.isNull(value)) {
            return false;
        }
        for (SexEnum sexEnum : values()) {
            if (Objects.equals(value, sexEnum.getValue())) {
                return true;
            }
        }
        return false;
    }
}
