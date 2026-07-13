package com.hanserwei.search.index;

/**
 * 用户索引结构常量.
 *
 * <p>对应 ES {@code user} 索引（见 {@code scripts/es-index/}）。小憨书号字段名为
 * {@code hannote_id}（非教程的 {@code xiaohashu_id}）。
 *
 * @author hanserwei
 * @date 2026/07/13
 * @since 0.0.1
 */
public final class UserIndex {

    private UserIndex() {
    }

    /** 索引名称 */
    public static final String NAME = "user";

    /** 用户 ID */
    public static final String FIELD_USER_ID = "id";
    /** 昵称 */
    public static final String FIELD_USER_NICKNAME = "nickname";
    /** 头像 */
    public static final String FIELD_USER_AVATAR = "avatar";
    /** 小憨书号 */
    public static final String FIELD_USER_HANNOTE_ID = "hannote_id";
    /** 发布笔记总数 */
    public static final String FIELD_USER_NOTE_TOTAL = "note_total";
    /** 粉丝总数 */
    public static final String FIELD_USER_FANS_TOTAL = "fans_total";
}
