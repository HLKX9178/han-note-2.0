package com.hanserwei.dataalign.constant;

/**
 * 日增量临时表命名常量.
 *
 * <p>临时表命名规则：{@code t_data_align_xxx_temp_{yyyyMMdd}_{分片序号}}，
 * 本类负责拼接「{日期}_{分片序号}」后缀。分片序号 = {@code id % 分片总数}。
 *
 * @author hanserwei
 * @date 2026/07/11
 * @since 0.0.1
 */
public final class TableConstants {

    private TableConstants() {
    }

    /**
     * 表名中的分隔符。
     */
    private static final String TABLE_NAME_SEPARATE = "_";

    /**
     * 拼接表名后缀：{日期}_{分片序号}。
     *
     * @param date    日期字符串（yyyyMMdd）
     * @param hashKey 分片序号（取模结果，long 以避免大 ID 取模类型问题）
     * @return 形如 {@code 20260711_0} 的后缀
     */
    public static String buildTableNameSuffix(String date, long hashKey) {
        return date + TABLE_NAME_SEPARATE + hashKey;
    }
}
