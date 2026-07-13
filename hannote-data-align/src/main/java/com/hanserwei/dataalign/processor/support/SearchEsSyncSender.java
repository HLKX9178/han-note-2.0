package com.hanserwei.dataalign.processor.support;

import com.hanserwei.dataalign.constant.MQConstants;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.client.producer.SendCallback;
import org.apache.rocketmq.client.producer.SendResult;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Component;

/**
 * 计数对齐后通知搜索服务刷新 ES 计数的发送器.
 *
 * <p>发「重建文档」事件到搜索服务已有的同步 Topic（与实时增量共用一套消费/重建逻辑）。
 * 顺序发送（hashKey=id），与笔记/用户服务的实时事件投递到同一队列，保证同一文档事件不乱序。
 * 尽力而为：发送失败仅记日志，不影响计数对齐主流程（重建是幂等的，下次对齐/实时事件会再刷新）。
 *
 * @author hanserwei
 * @date 2026/07/13
 * @since 0.0.1
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class SearchEsSyncSender {

    private final RocketMQTemplate rocketMQTemplate;

    /** 通知重建笔记文档（刷新 like_total / collect_total 等计数）。 */
    public void syncNote(long noteId) {
        send(MQConstants.TOPIC_NOTE_SYNC_ES + ":" + MQConstants.TAG_NOTE_REBUILD, noteId);
    }

    /** 通知重建用户文档（刷新 note_total / fans_total 等计数）。 */
    public void syncUser(long userId) {
        send(MQConstants.TOPIC_USER_SYNC_ES + ":" + MQConstants.TAG_USER_REBUILD, userId);
    }

    private void send(String destination, long id) {
        try {
            Message<String> message = MessageBuilder.withPayload(String.valueOf(id)).build();
            rocketMQTemplate.asyncSendOrderly(destination, message, String.valueOf(id), new SendCallback() {
                @Override
                public void onSuccess(SendResult sendResult) {
                    log.info("==> 【计数对齐→ES 同步】MQ 发送成功, destination: {}, id: {}", destination, id);
                }

                @Override
                public void onException(Throwable throwable) {
                    log.error("==> 【计数对齐→ES 同步】MQ 发送异常, destination: {}, id: {}", destination, id, throwable);
                }
            });
        } catch (Exception e) {
            log.error("==> 【计数对齐→ES 同步】MQ 发送失败, destination: {}, id: {}", destination, id, e);
        }
    }
}
