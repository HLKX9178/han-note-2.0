package com.hanserwei.dataalign.processor.support;

import com.hanserwei.dataalign.config.TableShardProperties;
import com.hanserwei.dataalign.domain.mapper.DeleteMapper;
import com.hanserwei.dataalign.domain.mapper.SelectMapper;
import com.hanserwei.dataalign.domain.mapper.UpdateMapper;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

/**
 * 分片对齐任务的共享协作者聚合.
 *
 * <p>把 8 个对齐处理器都要用到的 Mapper、RedisTemplate、分片数集中于一个 Bean，
 * 由抽象基类 {@code AbstractCountAlignProcessor} 与各子类共享，避免逐个处理器重复注入。
 *
 * @author hanserwei
 * @date 2026/07/11
 * @since 0.0.1
 */
@Component
@Getter
@RequiredArgsConstructor
public class CountAlignSupport {

    private final SelectMapper selectMapper;
    private final UpdateMapper updateMapper;
    private final DeleteMapper deleteMapper;
    private final RedisTemplate<String, Object> redisTemplate;
    private final TableShardProperties tableShardProperties;
    /** 计数对齐后通知搜索服务刷新 ES 计数 */
    private final SearchEsSyncSender searchEsSyncSender;
}
