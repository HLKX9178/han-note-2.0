package com.hanserwei.dataalign;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.serializer.GenericJacksonJsonRedisSerializer;
import tools.jackson.databind.json.JsonMapper;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * 计数缓存写入序列化一致性测试.
 *
 * <p>对齐任务用 {@code RedisTemplate.opsForHash().put(key, field, total)} 回写计数缓存，
 * 而 {@code hannote-count} 用 {@code HINCRBY} 维护同一 field（Redis 原生整数、纯字符串 {@code 5}）。
 * 若序列化器把 {@code Integer 5} 写成带类型包裹或带引号的形式，计数服务后续 {@code HINCRBY} 会
 * 报「hash value is not an integer」。本测试锁定：序列化结果必须是<strong>裸整数</strong>。
 *
 * @author hanserwei
 * @date 2026/07/11
 * @since 0.0.1
 */
class CountCacheSerializationTest {

    @Test
    @DisplayName("Integer 计数序列化为裸整数，兼容 HINCRBY")
    void integerCount_serializesToBareInteger() {
        JsonMapper jsonMapper = JsonMapper.builder().build();
        GenericJacksonJsonRedisSerializer serializer = new GenericJacksonJsonRedisSerializer(jsonMapper);

        byte[] bytes = serializer.serialize(5);
        String serialized = new String(bytes, StandardCharsets.UTF_8);

        assertEquals("5", serialized,
                "计数缓存字段必须写成裸整数以兼容 hannote-count 的 HINCRBY；实际=" + serialized);
    }
}
