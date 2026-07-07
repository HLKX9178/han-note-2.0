package com.hanserwei.auth.sms;

import com.aliyun.dypnsapi20170525.Client;
import com.aliyun.teaopenapi.models.Config;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 阿里云短信认证服务客户端配置.
 *
 * <p>基于 {@link AliyunAccessKeyProperties} 中的 AccessKey 初始化
 * {@link Client} Bean，供 {@link AliyunSmsHelper} 发送短信使用。
 *
 * <p>Endpoint 参考：<a href="https://api.aliyun.com/product/Dypnsapi">阿里云短信认证服务 API</a>
 *
 * @author hanserwei
 * @date 2026/07/07
 * @since 0.0.1
 */
@Configuration
@Slf4j
public class AliyunSmsClientConfig {

    @Resource
    private AliyunAccessKeyProperties aliyunAccessKeyProperties;

    /**
     * 创建阿里云短信认证服务客户端.
     *
     * @return Client；若初始化失败返回 null
     */
    @Bean
    public Client smsClient() {
        try {
            Config config = new Config()
                    .setAccessKeyId(aliyunAccessKeyProperties.getAccessKeyId())
                    .setAccessKeySecret(aliyunAccessKeyProperties.getAccessKeySecret());
            config.endpoint = "dypnsapi.aliyuncs.com";
            return new Client(config);
        } catch (Exception e) {
            log.error("初始化阿里云短信发送客户端错误: ", e);
            return null;
        }
    }
}
