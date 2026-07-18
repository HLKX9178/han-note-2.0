package com.hanserwei.kv.constant;

/**
 * 消息队列（RocketMQ）常量.
 *
 * @author hanserwei
 * @date 2026/07/18
 * @since 0.0.1
 */
public interface MQConstants {

    /**
     * Topic 主题：发布笔记事务消息（笔记服务生产，本服务消费写正文到 ScyllaDB）.
     */
    String TOPIC_PUBLISH_NOTE_TRANSACTION = "PublishNoteTransactionTopic";

    /**
     * 发布笔记事务消息消费者组.
     */
    String GROUP_SAVE_NOTE_CONTENT = "hannote_kv_save_note_content_group";
}
