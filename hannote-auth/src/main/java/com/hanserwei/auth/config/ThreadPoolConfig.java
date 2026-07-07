package com.hanserwei.auth.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.TaskExecutor;
import org.springframework.core.task.VirtualThreadTaskExecutor;

/**
 * 异步任务线程池配置.
 *
 * <p>使用 JDK 21+ 的虚拟线程（virtual threads）承载异步任务，具备以下特点：
 * <ul>
 *   <li>每个任务由一个新的虚拟线程执行，创建/销毁成本接近零；</li>
 *   <li>适合 IO 密集型任务（如调用阿里云短信 API）；</li>
 *   <li>不依赖固定大小的线程池，无需调优 corePoolSize / queueCapacity 等参数。</li>
 * </ul>
 *
 * <p>可通过 {@code hannote.async.virtual-threads=false} 关闭，回退到 Spring Boot 默认线程池。
 *
 * @author hanserwei
 * @date 2026/07/07
 * @since 0.0.1
 */
@Configuration
public class ThreadPoolConfig {

    /** 虚拟线程名前缀（便于日志与 jstack 排查） */
    private static final String THREAD_NAME_PREFIX = "AuthExecutor-";

    /**
     * 注册名为 {@code taskExecutor} 的虚拟线程执行器.
     *
     * @return 虚拟线程执行器
     */
    @Bean(name = "taskExecutor")
    @ConditionalOnProperty(name = "hannote.async.virtual-threads", havingValue = "true", matchIfMissing = true)
    public TaskExecutor virtualThreadTaskExecutor() {
        return new VirtualThreadTaskExecutor(THREAD_NAME_PREFIX);
    }
}
