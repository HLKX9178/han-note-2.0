package com.hanserwei.note.constant;

/**
 * 消息队列（RocketMQ）常量.
 *
 * <p>定义笔记服务用到的 Topic 主题等常量。
 *
 * @author hanserwei
 * @date 2026/07/09
 * @since 0.0.1
 */
public interface MQConstants {

    /**
     * Topic 主题：删除笔记本地缓存（广播模式，通知所有实例删各自 L1）
     */
    String TOPIC_DELETE_NOTE_LOCAL_CACHE = "DeleteNoteLocalCacheTopic";

    /**
     * Topic 主题：延迟双删 Redis 笔记缓存（集群模式，更新接口二次删 L2）
     */
    String TOPIC_DELAY_DELETE_NOTE_REDIS_CACHE = "DelayDeleteNoteRedisCacheTopic";

    /**
     * 广播删本地缓存消费者组（生产者与广播消费者共用）
     */
    String GROUP_DELETE_NOTE_LOCAL_CACHE = "hannote_group";

    /**
     * 延迟双删 Redis 缓存消费者组.
     *
     * <p>与广播消费者不同 Topic、不同消息模型，RocketMQ 要求同组订阅一致，
     * 故延时消费者独立成组。
     */
    String GROUP_DELAY_DELETE_NOTE_REDIS_CACHE = "hannote_group_delay_delete_redis_cache";

    /**
     * Topic 主题：笔记点赞 / 取消点赞（共用一个 Topic，用 Tag 区分）
     */
    String TOPIC_LIKE_UNLIKE = "LikeUnlikeTopic";

    /** Tag 标签：点赞 */
    String TAG_LIKE = "Like";

    /** Tag 标签：取消点赞 */
    String TAG_UNLIKE = "Unlike";

    /** 点赞 / 取消点赞落库消费者组（顺序消费） */
    String GROUP_LIKE_UNLIKE = "hannote_note_like_unlike_group";

    /**
     * Topic 主题：通知计数服务——笔记点赞数（笔记服务生产，计数服务消费）
     */
    String TOPIC_COUNT_NOTE_LIKE = "CountNoteLikeTopic";

    /**
     * Topic 主题：笔记收藏 / 取消收藏（共用一个 Topic，用 Tag 区分）
     */
    String TOPIC_COLLECT_UNCOLLECT = "CollectUnCollectTopic";

    /** Tag 标签：收藏 */
    String TAG_COLLECT = "Collect";

    /** Tag 标签：取消收藏 */
    String TAG_UNCOLLECT = "UnCollect";

    /** 收藏 / 取消收藏落库消费者组（顺序消费） */
    String GROUP_COLLECT_UNCOLLECT = "hannote_note_collect_uncollect_group";

    /**
     * Topic 主题：通知计数服务——笔记收藏数（笔记服务生产，计数服务消费）
     */
    String TOPIC_COUNT_NOTE_COLLECT = "CountNoteCollectTopic";

    /**
     * Topic 主题：笔记操作（发布 / 删除），用 Tag 区分，通知计数服务统计发布笔记数
     */
    String TOPIC_NOTE_OPERATE = "NoteOperateTopic";

    /** Tag 标签：笔记发布 */
    String TAG_NOTE_PUBLISH = "publishNote";

    /** Tag 标签：笔记删除 */
    String TAG_NOTE_DELETE = "deleteNote";

    /**
     * Topic 主题：笔记 ES 索引同步（笔记服务生产，搜索服务消费），用 Tag 区分重建 / 删除.
     *
     * <p>顺序发送（hashKey=noteId），保证同一笔记的 rebuild/delete 事件不乱序。
     */
    String TOPIC_NOTE_SYNC_ES = "NoteSyncEsTopic";

    /** Tag 标签：重建笔记 ES 文档（发布 / 编辑） */
    String TAG_SYNC_ES_REBUILD = "rebuild";

    /** Tag 标签：删除笔记 ES 文档（删除 / 转仅自己可见） */
    String TAG_SYNC_ES_DELETE = "delete";
}
