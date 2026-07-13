package com.hanserwei.search.enums;

import com.hanserwei.framework.common.exception.BaseExceptionInterface;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 搜索服务响应码.
 *
 * <p>错误码统一前缀 {@code SEARCH-}。本期（��仅落地通用异常码，
 * 业务异常码待后续按需补充（{@code SEARCH-2xxxx}）。
 *
 * @author hanserwei
 * @date 2026/07/13
 * @since 0.0.1
 */
@Getter
@AllArgsConstructor
public enum ResponseCodeEnum implements BaseExceptionInterface {

    // ----------- 通用异常状态码 -----------
    SYSTEM_ERROR("SEARCH-10000", "出错啦，请稍后再试~"),
    PARAM_NOT_VALID("SEARCH-10001", "参数错误"),

    // ----------- 业务异常状态码 -----------
    ;

    private final String errorCode;
    private final String errorMessage;
}
