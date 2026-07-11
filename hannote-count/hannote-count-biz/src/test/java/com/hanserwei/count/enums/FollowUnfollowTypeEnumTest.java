package com.hanserwei.count.enums;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class FollowUnfollowTypeEnumTest {

    @Test
    void of_shouldReturnEnumForValidCode() {
        assertEquals(FollowUnfollowTypeEnum.FOLLOW, FollowUnfollowTypeEnum.of(1));
        assertEquals(FollowUnfollowTypeEnum.UNFOLLOW, FollowUnfollowTypeEnum.of(0));
    }

    @Test
    void of_shouldReturnNullForInvalidOrNullCode() {
        assertNull(FollowUnfollowTypeEnum.of(99));
        assertNull(FollowUnfollowTypeEnum.of(null));
    }
}
