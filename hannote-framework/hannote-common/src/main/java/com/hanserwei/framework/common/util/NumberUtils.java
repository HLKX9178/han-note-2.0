package com.hanserwei.framework.common.util;

import java.math.RoundingMode;
import java.text.DecimalFormat;

/**
 * 数字展示工具.
 *
 * <p>将大数值计数（点赞、粉丝、收藏、评论等）转换为前端友好的展示字符串，
 * 如 {@code 137623 → "13.7万"}。搜索服务返参统一用其格式化各类计数。
 *
 * @author hanserwei
 * @date 2026/07/13
 * @since 0.0.1
 */
public final class NumberUtils {

    /** 一万 */
    private static final long TEN_THOUSAND = 10_000L;

    /** 一亿 */
    private static final long HUNDRED_MILLION = 100_000_000L;

    private NumberUtils() {
    }

    /**
     * 数值转展示字符串。
     *
     * <ul>
     *   <li>小于 1 万：原样展示，如 {@code 9999 → "9999"}；</li>
     *   <li>[1 万, 1 亿)：转为「万」并保留一位小数（截断，不四舍五入），如 {@code 137623 → "13.7万"}；</li>
     *   <li>大于等于 1 亿：统一展示 {@code "9999万"}。</li>
     * </ul>
     *
     * @param number 待格式化的数值（计数场景恒非负）
     * @return 展示字符串
     */
    public static String formatNumberString(long number) {
        if (number < TEN_THOUSAND) {
            return String.valueOf(number);
        }
        if (number < HUNDRED_MILLION) {
            // 保留一位小数并截断，避免出现 19999 → 2.0万 的向上取整
            DecimalFormat decimalFormat = new DecimalFormat("0.0");
            decimalFormat.setRoundingMode(RoundingMode.DOWN);
            return decimalFormat.format(number / (double) TEN_THOUSAND) + "万";
        }
        return "9999万";
    }
}
