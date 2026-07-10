package com.hanserwei.relation.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 异步执行器配置.
 *
 * <p>提供基于虚拟线程的 {@link ExecutorService}，用于关注/粉丝列表接口在缓存缺失回源后，
 * 异步将全量列表回填 Redis ZSET 等纯 IO 型任务。虚拟线程天然适配阻塞 IO，无需调优线程池参数
 * （对齐 note / user 服务的执行器）。
 *
 * @author hanserwei
 * @date 2026/07/10
 * @since 0.0.1
 */
@Configuration
public class AsyncConfig {

    /**
     * 用户关系服务异步执行器（每任务一虚拟线程）.
     *
     * @return 虚拟线程执行器
     */
    @Bean(name = "relationTaskExecutor", destroyMethod = "close")
    public ExecutorService relationTaskExecutor() {
        return Executors.newVirtualThreadPerTaskExecutor();
    }
}
