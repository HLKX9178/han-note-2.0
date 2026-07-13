package com.hanserwei.search.exception;

import com.hanserwei.framework.common.exception.BizException;
import com.hanserwei.framework.common.response.Response;
import com.hanserwei.search.enums.ResponseCodeEnum;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 搜索服务全局异常处理器.
 *
 * <p>统一将业务异常、参数校验异常、未知异常转换为
 * {@link com.hanserwei.framework.common.response.Response} 结构返回，避免暴露堆栈信息。
 * 错误码前缀 {@code SEARCH-}。
 *
 * @author hanserwei
 * @date 2026/07/13
 * @since 0.0.1
 */
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    /**
     * 捕获自定义业务异常。
     */
    @ExceptionHandler(BizException.class)
    public Response<Object> handleBizException(HttpServletRequest request, BizException e) {
        log.warn("{} request error, errorCode: {}, errorMessage: {}",
                request.getRequestURI(), e.getErrorCode(), e.getErrorMessage());
        return Response.fail(e);
    }

    /**
     * 捕获 Guava Preconditions 抛出的参数校验异常。
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public Response<Object> handleIllegalArgumentException(HttpServletRequest request, IllegalArgumentException e) {
        log.warn("{} request error, errorMessage: {}", request.getRequestURI(), e.getMessage());
        return Response.fail(ResponseCodeEnum.PARAM_NOT_VALID.getErrorCode(), e.getMessage());
    }

    /**
     * 捕获 {@code @Validated} 注解触发的参数校验异常，拼接各字段错误信息后返回。
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public Response<Object> handleMethodArgumentNotValidException(HttpServletRequest request,
                                                                  MethodArgumentNotValidException e) {
        StringBuilder sb = new StringBuilder();
        BindingResult bindingResult = e.getBindingResult();
        bindingResult.getFieldErrors().forEach(error ->
                sb.append(error.getField())
                        .append(' ')
                        .append(error.getDefaultMessage())
                        .append("; "));
        String errorMessage = sb.toString();

        log.warn("{} request param invalid: {}", request.getRequestURI(), errorMessage);
        return Response.fail(ResponseCodeEnum.PARAM_NOT_VALID.getErrorCode(), errorMessage);
    }

    /**
     * 兜底捕获其他所有未知异常，打印堆栈并返回系统错误码。
     */
    @ExceptionHandler(Exception.class)
    public Response<Object> handleOtherException(HttpServletRequest request, Exception e) {
        log.error("{} request error: ", request.getRequestURI(), e);
        return Response.fail(ResponseCodeEnum.SYSTEM_ERROR);
    }
}
