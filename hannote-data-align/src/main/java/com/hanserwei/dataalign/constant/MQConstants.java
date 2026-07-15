package com.hanserwei.dataalign.constant;

/**
 * 消息队列（RocketMQ）常量.
 *
 * <p>数据对齐服务消费计数链路已有的 5 个 Topic（Topic 名与 {@code hannote-count} 完全一致），
 * 但使用<strong>独立的 consumerGroup</strong>：不同 Group 各得一份完整消息副本，
 * 与计数服务并行消费、互不影响。
 *
 * @author hanserwei
 * @date 2026/07/11
 * @since 0.0.1
 */
public interface MQConstants {

    // ------------------------- Topic（复用 hannote-count 的 Topic 名） -------------------------

    /** Topic：笔记点赞数计数 */
    String TOPIC_COUNT_NOTE_LIKE = "CountNoteLikeTopic";

    /** Topic：笔记收藏数计数 */
    String TOPIC_COUNT_NOTE_COLLECT = "CountNoteCollectTopic";

    /** Topic：笔记操作（发布 / 删除），用 Tag 区分 */
    String TOPIC_NOTE_OPERATE = "NoteOperateTopic";

    /** Topic：关注数计数（对齐服务据此同时记录关注数、粉丝数两段日增量） */
    String TOPIC_COUNT_FOLLOWING = "CountFollowingTopic";

    /** Topic：评论真实新增/删除产生的笔记评论总数变更 */
    String TOPIC_COMMENT_COUNT_CHANGED = "CommentCountChangedTopic";

    // ------------------------- 消费者组（本服务专属，区别于计数服务） -------------------------

    /** 消费者组：点赞日增量落库 */
    String GROUP_DATA_ALIGN_NOTE_LIKE = "hannote_data_align_note_like_group";

    /** 消费者组：收藏日增量落库 */
    String GROUP_DATA_ALIGN_NOTE_COLLECT = "hannote_data_align_note_collect_group";

    /** 消费者组：笔记发布/删除日增量落库 */
    String GROUP_DATA_ALIGN_NOTE_OPERATE = "hannote_data_align_note_operate_group";

    /** 消费者组：关注/取关日增量落库 */
    String GROUP_DATA_ALIGN_FOLLOWING = "hannote_data_align_following_group";

    /** 消费者组：笔记评论总数日增量落库 */
    String GROUP_DATA_ALIGN_NOTE_COMMENT = "hannote_data_align_note_comment_group";

    // ------------------------- 生产者：计数对齐后通知搜索服务刷新 ES 计数 -------------------------
    // Topic / Tag 须与搜索服务 com.hanserwei.search.constant.MQConstants 逐字一致

    /** Topic：笔记 ES 索引同步 */
    String TOPIC_NOTE_SYNC_ES = "NoteSyncEsTopic";

    /** Tag：重建笔记 ES 文档 */
    String TAG_NOTE_REBUILD = "rebuild";

    /** Topic：用户 ES 索引同步 */
    String TOPIC_USER_SYNC_ES = "UserSyncEsTopic";

    /** Tag：仅重建用户 ES 文档 */
    String TAG_USER_REBUILD = "rebuildUser";
}
