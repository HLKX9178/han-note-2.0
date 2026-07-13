package com.hanserwei.search.constant;

/**
 * 消息队列（RocketMQ）常量.
 *
 * <p>搜索服务作为消费者，订阅笔记 / 用户变更事件做实时增量 ES 索引同步。
 * Topic / Tag 字符串须与生产者（笔记服务 {@code com.hanserwei.note.constant.MQConstants}、
 * 用户服务 {@code com.hanserwei.user.constant.MQConstants}、数据对齐服务）逐字一致。
 *
 * @author hanserwei
 * @date 2026/07/13
 * @since 0.0.1
 */
public interface MQConstants {

    /** Topic 主题：笔记 ES 索引同步（笔记服务 / 数据对齐服务生产） */
    String TOPIC_NOTE_SYNC_ES = "NoteSyncEsTopic";

    /** Tag 标签：重建笔记 ES 文档 */
    String TAG_NOTE_REBUILD = "rebuild";

    /** Tag 标签：删除笔记 ES 文档 */
    String TAG_NOTE_DELETE = "delete";

    /** 笔记 ES 同步消费者组（顺序消费） */
    String GROUP_NOTE_SYNC_ES = "hannote_search_note_sync_es_group";

    /** Topic 主题：用户 ES 索引同步（用户服务 / 数据对齐服务生产） */
    String TOPIC_USER_SYNC_ES = "UserSyncEsTopic";

    /** Tag 标签：仅重建用户 ES 文档 */
    String TAG_USER_REBUILD = "rebuildUser";

    /** Tag 标签：重建用户 ES 文档 + 该用户全部笔记文档 */
    String TAG_USER_REBUILD_AND_NOTES = "rebuildUserAndNotes";

    /** 用户 ES 同步消费者组（顺序消费） */
    String GROUP_USER_SYNC_ES = "hannote_search_user_sync_es_group";
}
