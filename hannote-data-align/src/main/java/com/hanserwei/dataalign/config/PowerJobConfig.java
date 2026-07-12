package com.hanserwei.dataalign.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import tech.powerjob.worker.PowerJobSpringWorker;
import tech.powerjob.worker.common.constants.StoreStrategy;

import java.util.Arrays;

/**
 * PowerJob Worker 手动初始化配置.
 *
 * <p>Spring Boot 4.1.0 与 {@code powerjob-worker-spring-boot-starter} 的自动装配存在兼容风险，
 * 故不引 starter，改按官方 <a href="https://www.yuque.com/powerjob/guidence/deploy_worker">deploy_worker</a>
 * 文档手动构造 {@link PowerJobSpringWorker}（版本 5.1.2）。
 *
 * <p>{@link PowerJobSpringWorker} 实现了 {@code InitializingBean}/{@code DisposableBean}/
 * {@code ApplicationContextAware}，注册为 Bean 后由 Spring 自动完成启动、优雅关闭，并从容器中
 * 发现 Spring 管理的处理器（{@code BasicProcessor}/{@code MapReduceProcessor}）。
 *
 * @author hanserwei
 * @date 2026/07/11
 * @since 0.0.1
 */
@Configuration
@RequiredArgsConstructor
@Slf4j
public class PowerJobConfig {

    private final PowerJobProperties properties;

    /**
     * 初始化 PowerJob 执行器（worker）。
     *
     * @return PowerJobSpringWorker Bean
     */
    @Bean
    public PowerJobSpringWorker powerJobSpringWorker() {
        tech.powerjob.worker.common.PowerJobWorkerConfig config =
                new tech.powerjob.worker.common.PowerJobWorkerConfig();
        config.setAppName(properties.getAppName());
        config.setServerAddress(Arrays.asList(properties.getServerAddress().split(",")));
        config.setPort(properties.getPort());
        config.setStoreStrategy("memory".equalsIgnoreCase(properties.getStoreStrategy())
                ? StoreStrategy.MEMORY : StoreStrategy.DISK);
        // server 暂不可达时也允许 worker 先启动，后续自动重连，避免联调期硬失败
        config.setAllowLazyConnectServer(true);

        log.info("==> PowerJob worker 初始化: appName={}, serverAddress={}, port={}, storeStrategy={}",
                properties.getAppName(), properties.getServerAddress(),
                properties.getPort(), properties.getStoreStrategy());

        return new PowerJobSpringWorker(config);
    }
}
