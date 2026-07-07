package com.hanserwei.oss.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * RustFS（S3 兼容对象存储）配置项.
 *
 * <p>绑定 {@code storage.rustfs.*}。
 *
 * @author hanserwei
 * @date 2026/07/07
 * @since 0.0.1
 */
@ConfigurationProperties(prefix = "rustfs")
@Component
@Data
public class RustFsProperties {

    /** 服务访问端点，如 http://127.0.0.1:9000 */
    private String endpoint;

    /** 访问密钥 ID */
    private String accessKey;

    /** 访问密钥 */
    private String secretKey;

    /** 区域（RustFS 不校验，可写死，如 us-east-1） */
    private String region = "us-east-1";

    /** 存储桶名称 */
    private String bucket;
}
