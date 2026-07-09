package com.hanserwei.note.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 异步执行器配置.
 *
 * <p>提供基于虚拟线程的 {@link ExecutorService}，用于笔记详情接口的下游服务并发调用
 * （{@code CompletableFuture}）以及缓存回写等纯 IO 型任务。虚拟线程天然适配阻塞 IO，
 * 无需调优线程池参数（对齐 user 服务 {@code cacheWriteExecutor}）。
 *
 * @author hanserwei
 * @date 2026/07/09
 * @since 0.0.1
 */
@Configuration
public class AsyncConfig {

    /**
     * 笔记服务异步执行器（每任务一虚拟线程）.
     *
     * @return 虚拟线程执行器
     */
    @Bean(name = "noteTaskExecutor", destroyMethod = "close")
    public ExecutorService noteTaskExecutor() {
        return Executors.newVirtualThreadPerTaskExecutor();
    }
}
