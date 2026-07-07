package com.hanserwei.gateway.enums;

import com.hanserwei.framework.common.exception.BaseExceptionInterface;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 网关服务响应码枚举.
 *
 * <p>错误码以 {@code GATEWAY-} 前缀区分服务来源。
 *
 * @author hanserwei
 * @date 2026/07/07
 * @since 0.0.1
 */
@Getter
@AllArgsConstructor
public enum ResponseCodeEnum implements BaseExceptionInterface {

    SYSTEM_ERROR("GATEWAY-10000", "网关繁忙，请稍后再试"),
    UNAUTHORIZED("GATEWAY-10001", "未登录或登录已过期"),
    TOKEN_INVALID("GATEWAY-10002", "无效的登录凭证"),
    FORBIDDEN("GATEWAY-10003", "权限不足");

    private final String errorCode;
    private final String errorMessage;
}
