package com.hanserwei.comment.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.core.task.SimpleAsyncTaskExecutor;

/**
 * 发送 MQ 失败后的异步重试执行器（虚拟线程）.
 *
 * <p>重试任务以「指数退避 sleep + 阻塞 syncSend」为主，是虚拟线程的理想场景。用
 * {@link SimpleAsyncTaskExecutor} 开启虚拟线程（每任务一线程、随创随销），并保留并发上限做背压——
 * 避免 MQ 长时间宕机时在途重试任务无界暴涨。
 *
 * <p>项目级 {@code spring.threads.virtual.enabled=true} 仅对 Spring 自动装配的 executor 生效，
 * 手动定义的执行器需显式开启虚拟线程，故此处显式 {@code setVirtualThreads(true)}。
 *
 * @author hanserwei
 * @date 2026/07/14
 * @since 0.0.1
 */
@Configuration
public class ThreadPoolConfig {

    /** 并发上限：MQ 宕机时限制在途重试任务数，超出则提交阻塞（背压） */
    private static final int CONCURRENCY_LIMIT = 200;

    @Bean(name = "taskExecutor")
    public AsyncTaskExecutor taskExecutor() {
        SimpleAsyncTaskExecutor executor = new SimpleAsyncTaskExecutor("CommentMqRetry-");
        executor.setVirtualThreads(true);
        executor.setConcurrencyLimit(CONCURRENCY_LIMIT);
        return executor;
    }
}
