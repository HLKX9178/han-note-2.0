package com.hanserwei.oss.strategy;

import org.springframework.web.multipart.MultipartFile;

/**
 * 文件存储策略接口.
 *
 * <p>策略模式：定义统一的文件操作契约，不同存储实现（RustFS / 腾讯云 COS）各自实现，
 * 调用方仅面向本接口编程，底层实现可按配置替换而无感知。
 *
 * @author hanserwei
 * @date 2026/07/07
 * @since 0.0.1
 */
public interface FileStrategy {

    /**
     * 上传文件.
     *
     * <p>目标存储桶由各实现从自身配置（{@code storage.*.bucket}）读取。
     *
     * @param file 待上传文件
     * @return 文件可访问 URL
     */
    String uploadFile(MultipartFile file);
}
