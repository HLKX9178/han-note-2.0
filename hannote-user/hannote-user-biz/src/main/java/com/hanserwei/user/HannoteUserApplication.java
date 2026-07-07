package com.hanserwei.user;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 用户服务启动类.
 *
 * @author hanserwei
 * @date 2026/07/07
 * @since 0.0.1
 */
@SpringBootApplication
@MapperScan("com.hanserwei.user.domain.mapper")
public class HannoteUserApplication {

    public static void main(String[] args) {
        SpringApplication.run(HannoteUserApplication.class, args);
    }
}
