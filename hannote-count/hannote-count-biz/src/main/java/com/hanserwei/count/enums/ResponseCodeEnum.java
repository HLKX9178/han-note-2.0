package com.hanserwei.count.enums;

import com.hanserwei.framework.common.exception.BaseExceptionInterface;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 计数服务响应码.
 *
 * <p>错误码统一前缀 {@code COUNT-}。本期（��仅落地通用异常码，
 * 业务异常码待后续接口落地时按需补充（{@code COUNT-2xxxx}）。
 *
 * @author hanserwei
 * @date 2026/07/11
 * @since 0.0.1
 */
@Getter
@AllArgsConstructor
public enum ResponseCodeEnum implements BaseExceptionInterface {

    // ----------- 通用异常状态码 -----------
    SYSTEM_ERROR("COUNT-10000", "出错啦，请稍后再试~"),
    PARAM_NOT_VALID("COUNT-10001", "参数错误"),

    // ----------- 业务异常状态码 -----------
    // 本期暂不定义业务错误码，待后续接口落地时按需补充（COUNT-2xxxx）
    ;

    private final String errorCode;
    private final String errorMessage;
}
