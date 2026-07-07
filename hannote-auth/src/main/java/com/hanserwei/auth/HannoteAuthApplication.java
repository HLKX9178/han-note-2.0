package com.hanserwei.auth;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

@SpringBootApplication
@MapperScan("com.hanserwei.auth.domain.mapper")
@EnableDiscoveryClient
public class HannoteAuthApplication {

    public static void main(String[] args) {
        SpringApplication.run(HannoteAuthApplication.class, args);
    }
}
