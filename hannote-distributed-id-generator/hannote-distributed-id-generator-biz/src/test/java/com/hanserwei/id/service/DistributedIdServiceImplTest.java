package com.hanserwei.id.service;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 分布式 ID 生成服务集成测试（依赖 Redis）.
 *
 * @author hanserwei
 * @date 2026/07/08
 * @since 0.0.1
 */
@SpringBootTest
class DistributedIdServiceImplTest {

    @Autowired
    private DistributedIdService distributedIdService;

    @Test
    void hannoteId_should_be_monotonically_increasing() {
        long first = distributedIdService.generateHannoteId();
        long second = distributedIdService.generateHannoteId();
        assertTrue(second > first, "hannote-id 应趋势递增");
        assertTrue(first >= 10000L, "hannote-id 起始值应 >= offset(10000)");
    }

    @Test
    void userId_should_be_monotonically_increasing() {
        long first = distributedIdService.generateUserId();
        long second = distributedIdService.generateUserId();
        assertTrue(second > first, "user-id 应趋势递增");
        assertTrue(first >= 10000L, "user-id 起始值应 >= offset(10000)");
    }
}
