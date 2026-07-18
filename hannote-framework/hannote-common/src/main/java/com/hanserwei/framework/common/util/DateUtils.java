package com.hanserwei.framework.common.util;

import com.hanserwei.framework.common.constant.DateConstants;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Period;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;

/**
 * 日期时间工具.
 *
 * <p>提供 {@link LocalDateTime} 与时间戳（epoch 毫秒）的通用换算、与 ES 索引日期串
 * （{@code yyyy-MM-dd HH:mm:ss}）的互转，以及展示友好的相对时间格式化，供各业务服务复用。
 * 典型场景：用户关系服务将关注时间作为 Redis ZSET 的 score 存储与排序；搜索服务将笔记
 * 更新时间格式化为「3小时前」「昨天 20:12」等相对时间返回前端。
 *
 * @author hanserwei
 * @date 2026/07/10
 * @since 0.0.1
 */
public final class DateUtils {

    private DateUtils() {
    }

    private static final DateTimeFormatter FMT_Y_M_D_H_M_S = DateTimeFormatter.ofPattern(DateConstants.Y_M_D_H_M_S);
    private static final DateTimeFormatter FMT_Y_M_D = DateTimeFormatter.ofPattern(DateConstants.Y_M_D);
    private static final DateTimeFormatter FMT_M_D = DateTimeFormatter.ofPattern(DateConstants.M_D);
    private static final DateTimeFormatter FMT_H_M = DateTimeFormatter.ofPattern(DateConstants.H_M);

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

    /**
     * {@link LocalDateTime} 转 {@code yyyy-MM-dd HH:mm:ss} 字符串。
     *
     * <p>用于构建 ES {@code range} 查询的时间边界值（索引中日期字段即此格式）。
     *
     * @param time 待格式化的时间，不可为 {@code null}
     * @return 格式化字符串
     */
    public static String localDateTime2String(LocalDateTime time) {
        return time.format(FMT_Y_M_D_H_M_S);
    }

    /**
     * 解析 ES 索引中的日期串（{@code yyyy-MM-dd HH:mm:ss}）为 {@link LocalDateTime}。
     *
     * @param text 日期字符串，不可为 {@code null}
     * @return 解析后的时间
     */
    public static LocalDateTime parseFromEsDateTime(String text) {
        return LocalDateTime.parse(text, FMT_Y_M_D_H_M_S);
    }

    /**
     * {@link LocalDateTime} 转展示友好的相对时间字符串。
     *
     * <p>规则（由近及远）：小于 1 分钟「刚刚」；小于 1 小时「x分钟前」；小于 24 小时
     * 「x小时前」；1 天前「昨天 HH:mm」；小于 7 天「x天前」；同年「MM-dd」；往年「yyyy-MM-dd」。
     * 未来时间统一按「刚刚」处理。
     *
     * @param dateTime 目标时间，不可为 {@code null}
     * @return 相对时间字符串
     */
    public static String formatRelativeTime(LocalDateTime dateTime) {
        LocalDateTime now = LocalDateTime.now();

        long minutes = ChronoUnit.MINUTES.between(dateTime, now);
        if (minutes < 1) {
            return "刚刚";
        }
        if (minutes < 60) {
            return minutes + "分钟前";
        }

        long hours = ChronoUnit.HOURS.between(dateTime, now);
        if (hours < 24) {
            return hours + "小时前";
        }

        long days = ChronoUnit.DAYS.between(dateTime, now);
        if (days < 2) {
            return "昨天 " + dateTime.format(FMT_H_M);
        }
        if (days < 7) {
            return days + "天前";
        }

        if (dateTime.getYear() == now.getYear()) {
            return dateTime.format(FMT_M_D);
        }
        return dateTime.format(FMT_Y_M_D);
    }

    /**
     * 根据出生日期计算周岁年龄.
     *
     * <p>以 {@link LocalDate#now()} 为基准，取完整年份数（当年生日未到则不计入）。
     *
     * @param birthDate 出生日期，不可为 {@code null}
     * @return 周岁年龄
     */
    public static int calculateAge(LocalDate birthDate) {
        return Period.between(birthDate, LocalDate.now()).getYears();
    }
}
