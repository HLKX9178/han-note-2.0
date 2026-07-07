package com.hanserwei.oss.config;

import com.qcloud.cos.COSClient;
import com.qcloud.cos.ClientConfig;
import com.qcloud.cos.auth.BasicCOSCredentials;
import com.qcloud.cos.auth.COSCredentials;
import com.qcloud.cos.region.Region;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 腾讯云 COS 客户端配置.
 *
 * <p>仅当 {@code storage.type=tencent-cos} 时装配 {@link COSClient}。
 *
 * @author hanserwei
 * @date 2026/07/07
 * @since 0.0.1
 */
@Configuration
@ConditionalOnProperty(name = "storage.type", havingValue = "tencent-cos")
@RequiredArgsConstructor
public class TencentCosClientConfig {

    private final TencentCosProperties tencentCosProperties;

    @Bean
    public COSClient cosClient() {
        COSCredentials credentials = new BasicCOSCredentials(
                tencentCosProperties.getSecretId(), tencentCosProperties.getSecretKey());
        ClientConfig clientConfig = new ClientConfig(new Region(tencentCosProperties.getRegion()));
        return new COSClient(credentials, clientConfig);
    }
}
