package com.hanserwei.gateway.exception;

import com.hanserwei.gateway.enums.ResponseCodeEnum;
import lombok.Getter;

/**
 * 网关未登录 / 令牌无效异常.
 *
 * <p>由鉴权过滤器在缺失令牌、令牌无效、令牌被登出（黑名单）时抛出，
 * 交由 {@link com.hanserwei.gateway.exception.GlobalExceptionHandler} 统一返回 401。
 *
 * @author hanserwei
 * @date 2026/07/07
 * @since 0.0.1
 */
@Getter
public class UnauthorizedException extends RuntimeException {

    private final String errorCode;

    public UnauthorizedException(ResponseCodeEnum codeEnum) {
        super(codeEnum.getErrorMessage());
        this.errorCode = codeEnum.getErrorCode();
    }
}
