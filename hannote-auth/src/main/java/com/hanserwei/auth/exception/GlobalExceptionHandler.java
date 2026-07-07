package com.hanserwei.auth.exception;

import com.hanserwei.auth.enums.ResponseCodeEnum;
import com.hanserwei.framework.common.exception.BizException;
import com.hanserwei.framework.common.response.Response;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 全局异常处理器.
 *
 * <p>统一将业务异常、参数校验异常、未知异常转换为
 * {@link com.hanserwei.framework.common.response.Response} 结构返回给客户端，
 * 避免暴露堆栈信息。
 *
 * @author hanserwei
 * @date 2026/07/07
 * @since 0.0.1
 */
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(BizException.class)
    @ResponseBody
    public Response<Object> handleBizException(HttpServletRequest request, BizException e) {
        log.warn("{} request error, errorCode: {}, errorMessage: {}",
                request.getRequestURI(), e.getErrorCode(), e.getErrorMessage());
        return Response.fail(e);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseBody
    public Response<Object> handleMethodArgumentNotValidException(HttpServletRequest request,
                                                                  MethodArgumentNotValidException e) {
        String errorCode = ResponseCodeEnum.PARAM_NOT_VALID.getErrorCode();

        StringBuilder sb = new StringBuilder();
        BindingResult bindingResult = e.getBindingResult();
        bindingResult.getFieldErrors().forEach(error ->
                sb.append(error.getField())
                        .append(' ')
                        .append(error.getDefaultMessage())
                        .append(", 当前值: '")
                        .append(error.getRejectedValue())
                        .append("'; "));
        String errorMessage = sb.toString();

        log.warn("{} request param invalid: {}", request.getRequestURI(), errorMessage);
        return Response.fail(errorCode, errorMessage);
    }

    @ExceptionHandler(Exception.class)
    @ResponseBody
    public Response<Object> handleOtherException(HttpServletRequest request, Exception e) {
        log.error("{} request error: ", request.getRequestURI(), e);
        return Response.fail(ResponseCodeEnum.SYSTEM_ERROR);
    }
}
