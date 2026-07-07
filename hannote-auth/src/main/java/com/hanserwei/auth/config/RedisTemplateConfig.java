package com.hanserwei.auth.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJacksonJsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import tools.jackson.databind.json.JsonMapper;

/**
 * RedisTemplate 配置.
 *
 * <p>Spring Data Redis 4.x 起推荐使用 {@link GenericJacksonJsonRedisSerializer}
 * （基于 Jackson 3 的 {@code tools.jackson.databind.json.JsonMapper}），替代已弃用的
 * {@code GenericJackson2JsonRedisSerializer}。
 *
 * <p>序列化策略：
 * <ul>
 *   <li>Key / HashKey：{@link StringRedisSerializer}（可读字符串）；</li>
 *   <li>Value / HashValue：{@link GenericJacksonJsonRedisSerializer}（JSON 格式，复用全局 {@link JsonMapper}）。</li>
 * </ul>
 *
 * @author hanserwei
 * @date 2026/07/07
 * @since 0.0.1
 */
@Configuration
public class RedisTemplateConfig {

    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory connectionFactory,
                                                       JsonMapper jsonMapper) {
        RedisTemplate<String, Object> redisTemplate = new RedisTemplate<>();
        redisTemplate.setConnectionFactory(connectionFactory);

        StringRedisSerializer stringSerializer = new StringRedisSerializer();
        redisTemplate.setKeySerializer(stringSerializer);
        redisTemplate.setHashKeySerializer(stringSerializer);

        // Spring Data Redis 4.x 推荐：基于 Jackson 3 的通用序列化器
        // 传入项目统一的 JsonMapper，确保序列化行为一致
        GenericJacksonJsonRedisSerializer jsonSerializer = new GenericJacksonJsonRedisSerializer(jsonMapper);
        redisTemplate.setValueSerializer(jsonSerializer);
        redisTemplate.setHashValueSerializer(jsonSerializer);

        redisTemplate.afterPropertiesSet();
        return redisTemplate;
    }
}
