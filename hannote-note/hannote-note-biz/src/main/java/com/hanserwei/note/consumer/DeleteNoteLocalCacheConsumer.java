package com.hanserwei.note.consumer;

import com.hanserwei.note.constant.MQConstants;
import com.hanserwei.note.service.NoteService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.annotation.MessageModel;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.stereotype.Component;

/**
 * 删除笔记本地缓存消费者（广播模式）.
 *
 * <p>笔记发生变更（更新/删除/仅自己可见/置顶）时，发布实例向本 Topic 发送广播消息，
 * 所有笔记服务实例都会收到并删除各自进程内的 L1（Caffeine）本地缓存，
 * 解决多实例本地缓存不一致问题。
 *
 * @author hanserwei
 * @date 2026/07/09
 * @since 0.0.1
 */
@Slf4j
@Component
@RequiredArgsConstructor
@RocketMQMessageListener(
        consumerGroup = MQConstants.GROUP_DELETE_NOTE_LOCAL_CACHE,
        topic = MQConstants.TOPIC_DELETE_NOTE_LOCAL_CACHE,
        messageModel = MessageModel.BROADCASTING)
public class DeleteNoteLocalCacheConsumer implements RocketMQListener<String> {

    private final NoteService noteService;

    @Override
    public void onMessage(String body) {
        Long noteId = Long.valueOf(body);
        log.info("## 广播消费：删除笔记本地缓存, noteId: {}", noteId);
        noteService.deleteNoteLocalCache(noteId);
    }
}
