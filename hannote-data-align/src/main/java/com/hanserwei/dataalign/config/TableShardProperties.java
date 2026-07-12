package com.hanserwei.dataalign.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.stereotype.Component;

/**
 * 日增量表分片数配置持有者.
 *
 * <p>用 {@code @RefreshScope} 承载 Nacos 配置中心的 {@code table.shards} 动态刷新，
 * 由消费者、分片任务注入后调用 {@link #getShards()} 读取<strong>当前</strong>值。
 *
 * <p>刻意<strong>不</strong>把 {@code @RefreshScope} 加在 PowerJob 处理器上：处理器由
 * PowerJob 按全限定类名解析，代理类名不一致会影响解析，故把可刷新代理隔离在本持有者上。
 *
 * @author hanserwei
 * @date 2026/07/11
 * @since 0.0.1
 */
@Component
@RefreshScope
public class TableShardProperties {

    /**
     * 日增量表分片总数（本地默认 3，可经 Nacos 配置中心覆盖）。
     */
    @Value("${table.shards:3}")
    private int shards;

    /**
     * 返回分片数，下限钳制为 1。
     *
     * <p>分片数用于取模（{@code id % shards}）与建表循环，配置为 0/负数没有业务意义且会引发
     * {@code ArithmeticException} 导致日增量静默丢失；此处钳制为单分片，保证系统始终可用。
     *
     * @return 有效分片数（≥ 1）
     */
    public int getShards() {
        return Math.max(shards, 1);
    }
}
