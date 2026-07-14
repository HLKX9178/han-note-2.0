package com.hanserwei.comment.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * PowerJob Worker 配置读取（前缀 {@code powerjob.worker}）.
 *
 * @author hanserwei
 * @date 2026/07/14
 * @since 0.0.1
 */
@Component
@ConfigurationProperties(prefix = PowerJobProperties.PREFIX)
@Data
public class PowerJobProperties {

    public static final String PREFIX = "powerjob.worker";

    /**
     * powerjob-server 部署地址（HTTP 端口默认 7700），多地址用英文逗号分隔。
     */
    private String serverAddress;

    /**
     * 应用名，需与 powerjob-server 控制台已注册的应用一致。
     */
    private String appName;

    /**
     * worker 通信端口。
     */
    private int port = 27777;

    /**
     * 任务信息存储策略：{@code disk} 落盘 / {@code memory} 纯内存。
     */
    private String storeStrategy = "disk";
}
