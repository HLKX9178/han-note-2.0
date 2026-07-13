package com.hanserwei.search.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Elasticsearch 连接配置项.
 *
 * <p>对应 {@code application-dev.yml} 中的 {@code elasticsearch.address}，格式为
 * {@code host:port}（本项目 dev 集群无鉴权）。
 *
 * @author hanserwei
 * @date 2026/07/13
 * @since 0.0.1
 */
@ConfigurationProperties(prefix = "elasticsearch")
@Component
@Data
public class ElasticsearchProperties {

    /** ES 地址，格式 {@code host:port}，如 {@code 192.168.1.117:9200} */
    private String address;
}
