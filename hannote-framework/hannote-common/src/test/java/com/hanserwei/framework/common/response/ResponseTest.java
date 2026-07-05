package com.hanserwei.framework.common.response;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ResponseTest {

    @Test
    void success_defaultsToSuccessTrue() {
        Response<String> r = Response.success("hi");
        assertTrue(r.isSuccess());
        assertEquals("hi", r.getData());
    }

    @Test
    void fail_withCodeAndMessage() {
        Response<Void> r = Response.fail("AUTH-10001", "参数错误");
        assertFalse(r.isSuccess());
        assertEquals("AUTH-10001", r.getErrorCode());
        assertEquals("参数错误", r.getMessage());
    }
}
