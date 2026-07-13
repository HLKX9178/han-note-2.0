package com.hanserwei.search.index;

/**
 * 笔记索引结构常量.
 *
 * <p>对应 ES {@code note} 索引（见 {@code scripts/es-index/}）。字段名以本项目已建索引为准：
 * 发布者字段是 {@code creator_nickname}/{@code creator_avatar}（非教程的 {@code nickname}/{@code avatar}）。
 *
 * @author hanserwei
 * @date 2026/07/13
 * @since 0.0.1
 */
public final class NoteIndex {

    private NoteIndex() {
    }

    /** 索引名称 */
    public static final String NAME = "note";

    /** 笔记 ID */
    public static final String FIELD_NOTE_ID = "id";
    /** 封面图 */
    public static final String FIELD_NOTE_COVER = "cover";
    /** 标题 */
    public static final String FIELD_NOTE_TITLE = "title";
    /** 话题 */
    public static final String FIELD_NOTE_TOPIC = "topic";
    /** 发布者昵称 */
    public static final String FIELD_NOTE_CREATOR_NICKNAME = "creator_nickname";
    /** 发布者头像 */
    public static final String FIELD_NOTE_CREATOR_AVATAR = "creator_avatar";
    /** 笔记类型（0 图文 / 1 视频） */
    public static final String FIELD_NOTE_TYPE = "type";
    /** 发布时间 */
    public static final String FIELD_NOTE_CREATE_TIME = "create_time";
    /** 更新时间 */
    public static final String FIELD_NOTE_UPDATE_TIME = "update_time";
    /** 被点赞数 */
    public static final String FIELD_NOTE_LIKE_TOTAL = "like_total";
    /** 被收藏数 */
    public static final String FIELD_NOTE_COLLECT_TOTAL = "collect_total";
    /** 被评论数 */
    public static final String FIELD_NOTE_COMMENT_TOTAL = "comment_total";
}
