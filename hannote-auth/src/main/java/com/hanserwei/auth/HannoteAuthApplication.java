package com.hanserwei.auth;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

@SpringBootApplication
@EnableDiscoveryClient
public class HannoteAuthApplication {

    public static void main(String[] args) {
        SpringApplication.run(HannoteAuthApplication.class, args);
    }
}
