package com.hanserwei.oss.config;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;

import java.net.URI;

/**
 * RustFS S3 客户端配置.
 *
 * <p>仅当 {@code storage.type=rustfs} 时装配 {@link S3Client}。
 * 按 RustFS 官方文档要求开启 {@code forcePathStyle}；region 任意（RustFS 不校验）。
 *
 * @author hanserwei
 * @date 2026/07/07
 * @since 0.0.1
 */
@Configuration
@ConditionalOnProperty(name = "storage.type", havingValue = "rustfs")
@RequiredArgsConstructor
public class RustFsClientConfig {

    private final RustFsProperties rustFsProperties;

    @Bean
    public S3Client rustFsS3Client() {
        return S3Client.builder()
                .endpointOverride(URI.create(rustFsProperties.getEndpoint()))
                .region(Region.of(rustFsProperties.getRegion()))
                .credentialsProvider(StaticCredentialsProvider.create(
                        AwsBasicCredentials.create(rustFsProperties.getAccessKey(), rustFsProperties.getSecretKey())))
                .forcePathStyle(true)
                .build();
    }
}
