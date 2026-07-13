package com.hanserwei.search;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 搜索服务启动类.
 *
 * <p>基于 Elasticsearch 提供笔记、用户的分词搜索能力，注册到 Nacos，端口 8089。
 *
 * @author hanserwei
 * @date 2026/07/13
 * @since 0.0.1
 */
@SpringBootApplication
public class HannoteSearchApplication {

    public static void main(String[] args) {
        SpringApplication.run(HannoteSearchApplication.class, args);
    }
}
