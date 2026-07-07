package com.hanserwei.gateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 网关服务启动类.
 *
 * <p>基于 Spring Cloud Gateway（WebFlux）实现路由转发、统一 JWT 鉴权与用户 ID 透传。
 *
 * @author hanserwei
 * @date 2026/07/07
 * @since 0.0.1
 */
@SpringBootApplication
public class HannoteGatewayApplication {

    public static void main(String[] args) {
        SpringApplication.run(HannoteGatewayApplication.class, args);
    }
}
