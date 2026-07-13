package com.hanserwei.search.config;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.json.jackson.JacksonJsonpMapper;
import co.elastic.clients.transport.rest5_client.Rest5ClientTransport;
import co.elastic.clients.transport.rest5_client.low_level.Rest5Client;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.net.URI;

/**
 * Elasticsearch 客户端配置.
 *
 * <p>使用 ES 官方新 Java 客户端（{@link ElasticsearchClient}），底层走 Apache HttpClient 5
 * 的 {@link Rest5Client} 传输层，JSON 序列化用 {@link JacksonJsonpMapper}（Jackson 2）。
 * 替代 ES 9.x 已移除的 {@code RestHighLevelClient}。
 *
 * @author hanserwei
 * @date 2026/07/13
 * @since 0.0.1
 */
@Configuration
@Slf4j
public class ElasticsearchConfig {

    /** 无鉴权 dev 集群，统一走 http */
    private static final String SCHEME = "http://";

    /**
     * 低阶 REST 客户端（Apache HttpClient 5）。容器关闭时自动 {@code close}。
     */
    @Bean(destroyMethod = "close")
    public Rest5Client rest5Client(ElasticsearchProperties properties) {
        URI uri = URI.create(SCHEME + properties.getAddress());
        log.info("==> 初始化 Elasticsearch Rest5Client, uri: {}", uri);
        return Rest5Client.builder(uri).build();
    }

    /**
     * ES 高阶类型安全客户端。
     */
    @Bean
    public ElasticsearchClient elasticsearchClient(Rest5Client rest5Client) {
        Rest5ClientTransport transport = new Rest5ClientTransport(rest5Client, new JacksonJsonpMapper());
        return new ElasticsearchClient(transport);
    }
}
