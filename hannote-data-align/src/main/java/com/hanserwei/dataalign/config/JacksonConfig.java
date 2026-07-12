package com.hanserwei.dataalign.config;

import com.hanserwei.framework.common.util.JsonUtils;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.SerializationFeature;
import tools.jackson.databind.json.JsonMapper;

/**
 * Jackson 配置.
 *
 * <p>提供全局 {@link JsonMapper}（Jackson 3），并注入 framework 层的 {@link JsonUtils}，
 * 使 RedisTemplate 序列化与 MQ 消息体解析行为保持一致。与 {@code hannote-count} 同款。
 *
 * @author hanserwei
 * @date 2026/07/11
 * @since 0.0.1
 */
@Configuration
public class JacksonConfig {

    @Bean
    public JsonMapper jsonMapper() {
        JsonMapper jsonMapper = JsonMapper.builder()
                // 反序列化时忽略未知字段，兼容各服务 MQ 消息体字段差异
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
                // 序列化空对象不报错
                .configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false)
                .build();

        // 将 Spring 管理的 JsonMapper 传播到 framework 层工具
        JsonUtils.init(jsonMapper);
        return jsonMapper;
    }
}
