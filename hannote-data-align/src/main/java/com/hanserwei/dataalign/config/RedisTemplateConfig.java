package com.hanserwei.dataalign.config;

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
 * <p>与 {@code hannote-count} 保持一致：Key/HashKey 用 {@link StringRedisSerializer}，
 * Value/HashValue 用基于 Jackson 3 的 {@link GenericJacksonJsonRedisSerializer}，
 * 使计数缓存回写（HSET followingTotal 等）与计数服务读到的结构完全兼容。
 *
 * <p>布隆过滤器相关的 {@code BF.*} 命令通过 Lua 脚本执行，不经该序列化器。
 *
 * @author hanserwei
 * @date 2026/07/11
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

        GenericJacksonJsonRedisSerializer jsonSerializer = new GenericJacksonJsonRedisSerializer(jsonMapper);
        redisTemplate.setValueSerializer(jsonSerializer);
        redisTemplate.setHashValueSerializer(jsonSerializer);

        redisTemplate.afterPropertiesSet();
        return redisTemplate;
    }
}
