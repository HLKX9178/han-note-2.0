package com.hanserwei.framework.common.util;

import java.time.LocalDateTime;
import java.time.ZoneOffset;

/**
 * 日期时间工具.
 *
 * <p>提供 {@link LocalDateTime} 与时间戳（epoch 毫秒）的通用换算，供各业务服务复用。
 * 典型场景：用户关系服务将关注时间作为 Redis ZSET 的 score 存储与排序。
 *
 * @author hanserwei
 * @date 2026/07/10
 * @since 0.0.1
 */
public final class DateUtils {

    private DateUtils() {
    }

    /**
     * {@link LocalDateTime} 转时间戳（epoch 毫秒）。
     *
     * <p>固定以 {@link ZoneOffset#UTC} 作为换算基准。用途是为 Redis ZSET 提供可排序的
     * score，只要「写入」与「回源」两处调用同一方法，相对顺序即可保持一致，不受部署时区影响。
     *
     * @param localDateTime 待转换的本地日期时间，不可为 {@code null}
     * @return 对应的 epoch 毫秒值
     */
    public static long localDateTime2Timestamp(LocalDateTime localDateTime) {
        return localDateTime.toInstant(ZoneOffset.UTC).toEpochMilli();
    }
}
