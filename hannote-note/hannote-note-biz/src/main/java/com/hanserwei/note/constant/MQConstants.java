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
}
