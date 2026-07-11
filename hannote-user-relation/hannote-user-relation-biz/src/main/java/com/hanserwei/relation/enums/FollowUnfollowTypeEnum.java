package com.hanserwei.relation.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

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
}
