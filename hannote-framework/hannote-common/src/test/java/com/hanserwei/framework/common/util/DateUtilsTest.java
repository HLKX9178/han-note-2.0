package com.hanserwei.framework.common.util;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;
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

    @Test
    void localDateTime2String_and_parse_roundTrip() {
        LocalDateTime dt = LocalDateTime.of(2026, 7, 13, 20, 12, 55);
        String text = DateUtils.localDateTime2String(dt);
        assertThat(text).isEqualTo("2026-07-13 20:12:55");
        assertThat(DateUtils.parseFromEsDateTime(text)).isEqualTo(dt);
    }

    @Test
    void formatRelativeTime_recentBuckets() {
        LocalDateTime now = LocalDateTime.now();
        assertThat(DateUtils.formatRelativeTime(now.minusSeconds(10))).isEqualTo("刚刚");
        assertThat(DateUtils.formatRelativeTime(now.minusMinutes(5))).isEqualTo("5分钟前");
        assertThat(DateUtils.formatRelativeTime(now.minusHours(3))).isEqualTo("3小时前");
        assertThat(DateUtils.formatRelativeTime(now.minusDays(3))).isEqualTo("3天前");
        // 未来时间按「刚刚」兜底
        assertThat(DateUtils.formatRelativeTime(now.plusMinutes(5))).isEqualTo("刚刚");
    }

    @Test
    void formatRelativeTime_yesterdayShowsHourMinute() {
        LocalDateTime yesterday = LocalDateTime.now().minusHours(30);
        assertThat(DateUtils.formatRelativeTime(yesterday)).startsWith("昨天 ");
    }

    @Test
    void formatRelativeTime_pastYearShowsFullDate() {
        LocalDateTime old = LocalDateTime.of(2023, 12, 1, 8, 0, 0);
        assertThat(DateUtils.formatRelativeTime(old)).isEqualTo("2023-12-01");
    }

    @Test
    void calculateAge_shouldReturnFullYears() {
        // 20 年前的今天出生 → 20 岁
        LocalDate birth = LocalDate.now().minusYears(20);
        assertThat(DateUtils.calculateAge(birth)).isEqualTo(20);
    }

    @Test
    void calculateAge_shouldNotCountBirthdayNotYetReached() {
        // 20 年前 + 明天生日（今年还没到）→ 19 岁
        LocalDate birth = LocalDate.now().minusYears(20).plusDays(1);
        assertThat(DateUtils.calculateAge(birth)).isEqualTo(19);
    }
}
