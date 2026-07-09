package com.hanserwei.note.enums;

import com.hanserwei.framework.common.exception.BaseExceptionInterface;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 笔记服务响应码.
 *
 * @author hanserwei
 * @date 2026/07/09
 * @since 0.0.1
 */
@Getter
@AllArgsConstructor
public enum ResponseCodeEnum implements BaseExceptionInterface {

    // ----------- 通用异常状态码 -----------
    SYSTEM_ERROR("NOTE-10000", "出错啦，请稍后再试~"),
    PARAM_NOT_VALID("NOTE-10001", "参数错误"),

    // ----------- 业务异常状态码 -----------
    // 后续博客出业务接口时按需补充（NOTE-2xxxx）
    ;

    private final String errorCode;
    private final String errorMessage;
}
