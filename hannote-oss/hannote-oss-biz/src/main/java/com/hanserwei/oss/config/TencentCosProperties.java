package com.hanserwei.oss.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 腾讯云 COS 配置项.
 *
 * <p>绑定 {@code storage.tencent-cos.*}。
 *
 * @author hanserwei
 * @date 2026/07/07
 * @since 0.0.1
 */
@ConfigurationProperties(prefix = "tencent.cos")
@Component
@Data
public class TencentCosProperties {

    /** 地域，如 ap-guangzhou */
    private String region;

    /** 访问密钥 SecretId */
    private String secretId;

    /** 访问密钥 SecretKey */
    private String secretKey;

    /** 存储桶名称（含 APPID 后缀，如 hannote-1250000000） */
    private String bucket;

    /** 自定义访问域名（可选，配置后返回的 URL 使用该域名，如 https://note.likeyy.love） */
    private String customDomain;
}
