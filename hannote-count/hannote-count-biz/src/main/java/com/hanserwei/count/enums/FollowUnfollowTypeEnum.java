package com.hanserwei.count.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Objects;

/**
 * 关注 / 取关操作类型.
 *
 * @author hanserwei
 * @date 2026/07/11
 * @since 0.0.1
 */
@Getter
@AllArgsConstructor
public enum FollowUnfollowTypeEnum {

    /** 关注 */
    FOLLOW(1),
    /** 取关 */
    UNFOLLOW(0);

    private final Integer code;

    /**
     * 按 code 反查枚举。
     *
     * @param code 操作类型 code
     * @return 匹配的枚举；无匹配返回 {@code null}
     */
    public static FollowUnfollowTypeEnum of(Integer code) {
        for (FollowUnfollowTypeEnum typeEnum : values()) {
            if (Objects.equals(code, typeEnum.getCode())) {
                return typeEnum;
            }
        }
        return null;
    }
}
