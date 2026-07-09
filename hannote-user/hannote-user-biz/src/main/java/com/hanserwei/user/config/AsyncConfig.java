package com.hanserwei.user.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 异步执行器配置.
 *
 * <p>提供基于虚拟线程的 {@link ExecutorService}，用于缓存回写等纯 IO 型的
 * fire-and-forget 任务。虚拟线程天然适配阻塞 IO，无需调优线程池参数，
 * 也避免在压测洪峰下回落主线程拖慢响应。
 *
 * @author hanserwei
 * @date 2026/07/09
 * @since 0.0.1
 */
@Configuration
public class AsyncConfig {

    /**
     * 缓存回写执行器（每任务一虚拟线程）.
     *
     * @return 虚拟线程执行器
     */
    @Bean(name = "cacheWriteExecutor", destroyMethod = "close")
    public ExecutorService cacheWriteExecutor() {
        return Executors.newVirtualThreadPerTaskExecutor();
    }
}
