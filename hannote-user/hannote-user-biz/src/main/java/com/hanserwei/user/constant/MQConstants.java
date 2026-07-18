package com.hanserwei.user.constant;

/**
 * 消息队列（RocketMQ）常量.
 *
 * <p>定义用户服务用到的 Topic 主题等常量。当前仅用于用户变更后通知搜索服务重建 ES 索引。
 *
 * @author hanserwei
 * @date 2026/07/13
 * @since 0.0.1
 */
public interface MQConstants {

    /**
     * Topic 主题：用户 ES 索引同步（用户服务生产，搜索服务消费），用 Tag 区分.
     *
     * <p>顺序发送（hashKey=userId），保证同一用户的同步事件不乱序。
     */
    String TOPIC_USER_SYNC_ES = "UserSyncEsTopic";

    /** Tag 标签：仅重建用户 ES 文档（注册 / 昵称头像未变的资料修改） */
    String TAG_SYNC_ES_REBUILD_USER = "rebuildUser";

    /** Tag 标签：重建用户 ES 文档 + 该用户全部笔记文档（昵称 / 头像变更，笔记索引冗余了发布者信息） */
    String TAG_SYNC_ES_REBUILD_USER_AND_NOTES = "rebuildUserAndNotes";

    /**
     * Topic 主题：延迟双删 Redis 用户缓存（集群模式，更新接口二次删 L2）.
     */
    String TOPIC_DELAY_DELETE_USER_REDIS_CACHE = "DelayDeleteUserRedisCacheTopic";

    /**
     * 延迟双删 Redis 用户缓存消费者组.
     */
    String GROUP_DELAY_DELETE_USER_REDIS_CACHE = "hannote_group_delay_delete_user_redis_cache";
}
