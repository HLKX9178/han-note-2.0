package com.hanserwei.comment.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import tech.powerjob.worker.PowerJobSpringWorker;
import tech.powerjob.worker.common.constants.StoreStrategy;

import java.util.Arrays;

/**
 * PowerJob Worker 手动初始化配置（同 hannote-data-align 做法）.
 *
 * <p>不引 starter，按官方文档手动构造 {@link PowerJobSpringWorker}（版本 5.1.2）。注册为 Bean 后由
 * Spring 完成启动/优雅关闭，并从容器中发现 Spring 管理的处理器（{@code BasicProcessor}）。
 *
 * <p>用于评论发布发送失败 MQ 的兜底扫表重发（见 {@code processor/MqResendProcessor}）。
 *
 * @author hanserwei
 * @date 2026/07/14
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
