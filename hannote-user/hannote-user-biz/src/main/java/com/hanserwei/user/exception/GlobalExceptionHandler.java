package com.hanserwei.user.exception;

import com.hanserwei.framework.common.exception.BizException;
import com.hanserwei.framework.common.response.Response;
import com.hanserwei.user.enums.ResponseCodeEnum;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 用户服务全局异常处理器.
 *
 * <p>统一将业务异常、参数校验异常、未知异常转换为
 * {@link com.hanserwei.framework.common.response.Response} 结构返回，避免暴露堆栈信息。
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

    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseBody
    public Response<Object> handleIllegalArgumentException(HttpServletRequest request, IllegalArgumentException e) {
        log.warn("{} request error, errorMessage: {}", request.getRequestURI(), e.getMessage());
        return Response.fail(ResponseCodeEnum.SYSTEM_ERROR.getErrorCode(), e.getMessage());
    }

    @ExceptionHandler({MethodArgumentNotValidException.class, org.springframework.validation.BindException.class})
    @ResponseBody
    public Response<Object> handleControllerException(HttpServletRequest request, Throwable e) {
        BindingResult bindingResult = null;
        if (e instanceof MethodArgumentNotValidException ex) {
            bindingResult = ex.getBindingResult();
        } else if (e instanceof org.springframework.validation.BindException ex) {
            bindingResult = ex.getBindingResult();
        }

        StringBuilder sb = new StringBuilder();
        if (bindingResult != null) {
            bindingResult.getFieldErrors().forEach(error ->
                    sb.append(error.getField())
                            .append(' ')
                            .append(error.getDefaultMessage())
                            .append("; "));
        }
        String errorMessage = sb.toString();

        log.warn("{} request param invalid: {}", request.getRequestURI(), errorMessage);
        return Response.fail(ResponseCodeEnum.SYSTEM_ERROR.getErrorCode(), errorMessage);
    }

    @ExceptionHandler(Exception.class)
    @ResponseBody
    public Response<Object> handleOtherException(HttpServletRequest request, Exception e) {
        log.error("{} request error: ", request.getRequestURI(), e);
        return Response.fail(ResponseCodeEnum.SYSTEM_ERROR);
    }
}
