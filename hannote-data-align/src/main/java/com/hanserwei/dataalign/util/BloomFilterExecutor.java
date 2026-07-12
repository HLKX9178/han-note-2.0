package com.hanserwei.dataalign.util;

import lombok.RequiredArgsConstructor;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.scripting.support.ResourceScriptSource;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.Objects;

/**
 * 日增量布隆过滤器执行器.
 *
 * <p>统一封装两段 Lua：判重（不存在则初始化+过期）与追加。与 {@code hannote-note} 一致，
 * 使用 {@link StringRedisTemplate} + {@code String.valueOf(value)}，保证 {@code BF.ADD} 与
 * {@code BF.EXISTS} 使用相同的字符串序列化、结果一致。依赖 RedisBloom 模块（项目已就绪）。
 *
 * @author hanserwei
 * @date 2026/07/11
 * @since 0.0.1
 */
@Component
@RequiredArgsConstructor
public class BloomFilterExecutor {

    private final StringRedisTemplate stringRedisTemplate;

    private static final DefaultRedisScript<Long> CHECK_SCRIPT = buildScript("/lua/bloom_today_check.lua");
    private static final DefaultRedisScript<Long> ADD_SCRIPT = buildScript("/lua/bloom_add.lua");

    private static DefaultRedisScript<Long> buildScript(String path) {
        DefaultRedisScript<Long> script = new DefaultRedisScript<>();
        script.setScriptSource(new ResourceScriptSource(new ClassPathResource(path)));
        script.setResultType(Long.class);
        return script;
    }

    /**
     * 判断值是否<strong>一定不存在</strong>于布隆过滤器（返回 0）。
     *
     * <p>布隆「不存在」判定绝对正确，返回 {@code true} 表示该变更尚未记录、应落库。
     *
     * @param bloomKey 布隆过滤器 Key
     * @param value    待判重的值（userId / noteId）
     * @return true = 一定未记录；false = 可能已记录（跳过落库）
     */
    public boolean isAbsent(String bloomKey, Object value) {
        Long result = stringRedisTemplate.execute(CHECK_SCRIPT,
                Collections.singletonList(bloomKey), String.valueOf(value));
        return Objects.equals(result, 0L);
    }

    /**
     * 将值加入布隆过滤器（落库成功后调用）。
     *
     * @param bloomKey 布隆过滤器 Key
     * @param value    待加入的值
     */
    public void add(String bloomKey, Object value) {
        stringRedisTemplate.execute(ADD_SCRIPT,
                Collections.singletonList(bloomKey), String.valueOf(value));
    }
}
