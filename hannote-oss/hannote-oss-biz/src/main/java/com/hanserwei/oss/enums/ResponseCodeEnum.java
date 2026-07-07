package com.hanserwei.oss.enums;

import com.hanserwei.framework.common.exception.BaseExceptionInterface;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 对象存储服务响应码枚举.
 *
 * <p>错误码以 {@code OSS-} 前缀区分服务来源。
 *
 * @author hanserwei
 * @date 2026/07/07
 * @since 0.0.1
 */
@Getter
@AllArgsConstructor
public enum ResponseCodeEnum implements BaseExceptionInterface {

    SYSTEM_ERROR("OSS-10000", "出错啦，后台小哥正在努力修复中..."),
    FILE_EMPTY("OSS-10001", "上传文件不能为空"),
    FILE_UPLOAD_FAILED("OSS-10002", "文件上传失败");

    private final String errorCode;
    private final String errorMessage;
}
