package com.hanserwei.dataalign;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 数据对齐服务启动类.
 *
 * <p>默默干活的后台服务：消费计数链路 MQ 落库「日增量变更表」，并由 PowerJob 定时分片任务
 * 从源头表重新 {@code count(*)} 真实值，回写计数表与 Redis 缓存，纠正计数漂移、保证最终一致。
 * 无对外 HTTP/RPC 接口。
 *
 * @author hanserwei
 * @date 2026/07/11
 * @since 0.0.1
 */
@SpringBootApplication
@MapperScan("com.hanserwei.dataalign.domain.mapper")
public class HannoteDataAlignApplication {

    public static void main(String[] args) {
        SpringApplication.run(HannoteDataAlignApplication.class, args);
    }
}
