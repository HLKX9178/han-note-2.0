package com.hanserwei.count.enums;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class FollowUnfollowTypeEnumTest {

    @Test
    void valueOf_shouldReturnEnumForValidCode() {
        assertEquals(FollowUnfollowTypeEnum.FOLLOW, FollowUnfollowTypeEnum.valueOf(1));
        assertEquals(FollowUnfollowTypeEnum.UNFOLLOW, FollowUnfollowTypeEnum.valueOf(0));
    }

    @Test
    void valueOf_shouldReturnNullForInvalidOrNullCode() {
        assertNull(FollowUnfollowTypeEnum.valueOf(99));
        assertNull(FollowUnfollowTypeEnum.valueOf((Integer) null));
    }
}
