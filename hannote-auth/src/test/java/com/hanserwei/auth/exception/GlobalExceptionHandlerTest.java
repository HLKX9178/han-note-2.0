package com.hanserwei.auth.exception;

import com.hanserwei.auth.enums.ResponseCodeEnum;
import com.hanserwei.framework.common.exception.BizException;
import com.hanserwei.framework.common.response.Response;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();
    private final HttpServletRequest request = new MockHttpServletRequest();

    @Test
    void handleBizException_returnsFailWithCode() {
        BizException e = new BizException(ResponseCodeEnum.SYSTEM_ERROR);
        Response<Object> r = handler.handleBizException(request, e);
        assertFalse(r.isSuccess());
        assertEquals("AUTH-10000", r.getErrorCode());
    }

    @Test
    void handleOtherException_returnsSystemError() {
        Response<Object> r = handler.handleOtherException(request, new RuntimeException("boom"));
        assertFalse(r.isSuccess());
        assertEquals("AUTH-10000", r.getErrorCode());
    }
}
