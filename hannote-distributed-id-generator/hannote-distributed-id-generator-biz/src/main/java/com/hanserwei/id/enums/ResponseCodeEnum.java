package com.hanserwei.id.enums;

import com.hanserwei.framework.common.exception.BaseExceptionInterface;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 分布式 ID 生成服务响应码枚举.
 *
 * <p>错误码以 {@code ID-} 前缀区分服务来源。
 *
 * @author hanserwei
 * @date 2026/07/08
 * @since 0.0.1
 */
@Getter
@AllArgsConstructor
public enum ResponseCodeEnum implements BaseExceptionInterface {

    // ----------- 通用异常状态码 -----------
    SYSTEM_ERROR("ID-10000", "出错啦，后台小哥正在努力修复中..."),
    PARAM_NOT_VALID("ID-10001", "参数错误"),

    // ----------- 业务异常状态码 -----------
    ID_GENERATE_FAIL("ID-20000", "分布式 ID 生成失败");

    private final String errorCode;
    private final String errorMessage;
}
