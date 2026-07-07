package com.hanserwei.auth.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 用户登录类型枚举.
 *
 * <p>支持多种登录方式：验证码登录、密码登录。
 *
 * @author hanserwei
 * @date 2026/07/07
 * @since 0.0.1
 */
@Getter
@AllArgsConstructor
public enum LoginTypeEnum {

    /** 手机号 + 验证码登录 */
    VERIFICATION_CODE(1),

    /** 账号 + 密码登录（阶段一未启用） */
    PASSWORD(2);

    /** 登录类型编码 */
    private final Integer code;

    /**
     * 根据 code 获取枚举实例.
     *
     * @param code 登录类型 code
     * @return 匹配的枚举实例；若不匹配返回 null
     */
    public static LoginTypeEnum of(Integer code) {
        for (LoginTypeEnum type : values()) {
            if (type.getCode().equals(code)) {
                return type;
            }
        }
        return null;
    }
}
