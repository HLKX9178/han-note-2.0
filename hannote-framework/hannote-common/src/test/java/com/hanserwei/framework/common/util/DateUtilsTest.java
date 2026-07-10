package com.hanserwei.framework.common.util;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;

class DateUtilsTest {

    @Test
    void localDateTime2Timestamp_matchesUtcEpochMilli() {
        // 1970-01-01T00:00:00 UTC 对应 epoch 毫秒 0
        LocalDateTime epoch = LocalDateTime.of(1970, 1, 1, 0, 0, 0);
        assertThat(DateUtils.localDateTime2Timestamp(epoch)).isZero();

        // 任意时间点：与 UTC 基准换算结果一致
        LocalDateTime dt = LocalDateTime.of(2026, 7, 10, 12, 0, 0);
        long expected = dt.toInstant(ZoneOffset.UTC).toEpochMilli();
        assertThat(DateUtils.localDateTime2Timestamp(dt)).isEqualTo(expected);
    }

    @Test
    void localDateTime2Timestamp_isMonotonicWithTime() {
        // 较晚的时间戳必然更大，保证可用于 ZSET 排序
        LocalDateTime earlier = LocalDateTime.of(2026, 7, 10, 12, 0, 0);
        LocalDateTime later = LocalDateTime.of(2026, 7, 10, 12, 0, 1);
        assertThat(DateUtils.localDateTime2Timestamp(later))
                .isGreaterThan(DateUtils.localDateTime2Timestamp(earlier));
    }
}
