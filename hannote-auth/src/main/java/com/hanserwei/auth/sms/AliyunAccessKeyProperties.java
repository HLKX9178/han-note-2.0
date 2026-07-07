package com.hanserwei.auth.sms;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 阿里云接入凭证与短信配置.
 *
 * <p>绑定 {@code application-dev.yml} 中 {@code aliyun.*} 命名空间下的配置：
 * <pre>
 * aliyun:
 *   access-key-id: xxx
 *   access-key-secret: xxx
 *   sms:
 *     sign-name: "xxx"
 *     template-code: "xxx"
 * </pre>
 *
 * @author hanserwei
 * @date 2026/07/07
 * @since 0.0.1
 */
@ConfigurationProperties(prefix = "aliyun")
@Component
@Data
public class AliyunAccessKeyProperties {

    /** 阿里云 AccessKey ID */
    private String accessKeyId;

    /** 阿里云 AccessKey Secret */
    private String accessKeySecret;

    /** 短信相关配置 */
    private Sms sms = new Sms();

    /**
     * 短信配置子项.
     */
    @Data
    public static class Sms {

        /** 短信签名（阿里云后台申请） */
        private String signName;

        /** 短信模板编码（阿里云后台申请） */
        private String templateCode;
    }
}
