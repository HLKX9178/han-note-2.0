package com.hanserwei.search.consumer;

import com.hanserwei.search.constant.MQConstants;
import com.hanserwei.search.service.EsSyncService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.common.message.MessageExt;
import org.apache.rocketmq.spring.annotation.ConsumeMode;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.Objects;

/**
 * 笔记 ES 索引同步消费者.
 *
 * <p>消费笔记服务 / 数据对齐服务发布的笔记变更事件，按 Tag 重建或删除笔记 ES 文档。
 * 顺序消费（同一 noteId 的事件由生产端顺序投递到同一队列），保证 rebuild/delete 不乱序。
 *
 * @author hanserwei
 * @date 2026/07/13
 * @since 0.0.1
 */
@Component
@Slf4j
@RequiredArgsConstructor
@RocketMQMessageListener(
        topic = MQConstants.TOPIC_NOTE_SYNC_ES,
        consumerGroup = MQConstants.GROUP_NOTE_SYNC_ES,
        consumeMode = ConsumeMode.ORDERLY)
public class NoteSyncEsConsumer implements RocketMQListener<MessageExt> {

    private final EsSyncService esSyncService;

    @Override
    public void onMessage(MessageExt message) {
        String body = new String(message.getBody(), StandardCharsets.UTF_8);
        String tag = message.getTags();
        log.info("==> NoteSyncEsConsumer 消费消息, noteId: {}, tag: {}", body, tag);

        Long noteId = Long.valueOf(body);
        if (Objects.equals(tag, MQConstants.TAG_NOTE_REBUILD)) {
            esSyncService.rebuildNote(noteId);
        } else if (Objects.equals(tag, MQConstants.TAG_NOTE_DELETE)) {
            esSyncService.deleteNote(noteId);
        } else {
            log.warn("==> NoteSyncEsConsumer 未知 tag: {}", tag);
        }
    }
}
