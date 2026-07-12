package com.hanserwei.dataalign.constant;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * {@link TableConstants} 单元测试.
 *
 * @author hanserwei
 * @date 2026/07/11
 * @since 0.0.1
 */
class TableConstantsTest {

    @Test
    @DisplayName("表名后缀 = 日期_分片序号")
    void buildTableNameSuffix_joinsDateAndShard() {
        assertEquals("20260711_0", TableConstants.buildTableNameSuffix("20260711", 0));
        assertEquals("20260711_2", TableConstants.buildTableNameSuffix("20260711", 2));
    }

    @Test
    @DisplayName("分片序号由 id % 分片总数 得到")
    void buildTableNameSuffix_withModuloShardIndex() {
        int shards = 3;
        // 10 % 3 = 1，落到 1 号分片表
        assertEquals("20260711_1", TableConstants.buildTableNameSuffix("20260711", 10L % shards));
        // 9 % 3 = 0，落到 0 号分片表
        assertEquals("20260711_0", TableConstants.buildTableNameSuffix("20260711", 9L % shards));
        // 大雪花 ID 取模仍为 long，不溢出
        long bigId = 7_300_000_000_000_000_000L;
        assertEquals("20260711_" + (bigId % shards),
                TableConstants.buildTableNameSuffix("20260711", bigId % shards));
    }
}
