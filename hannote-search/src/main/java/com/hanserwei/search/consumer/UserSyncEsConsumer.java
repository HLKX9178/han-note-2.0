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
 * 用户 ES 索引同步消费者.
 *
 * <p>消费用户服务 / 数据对齐服务发布的用户变更事件，按 Tag 重建用户文档，或连带重建该用户
 * 全部笔记文档（昵称 / 头像变更，笔记索引冗余了发布者信息）。顺序消费。
 *
 * @author hanserwei
 * @date 2026/07/13
 * @since 0.0.1
 */
@Component
@Slf4j
@RequiredArgsConstructor
@RocketMQMessageListener(
        topic = MQConstants.TOPIC_USER_SYNC_ES,
        consumerGroup = MQConstants.GROUP_USER_SYNC_ES,
        consumeMode = ConsumeMode.ORDERLY)
public class UserSyncEsConsumer implements RocketMQListener<MessageExt> {

    private final EsSyncService esSyncService;

    @Override
    public void onMessage(MessageExt message) {
        String body = new String(message.getBody(), StandardCharsets.UTF_8);
        String tag = message.getTags();
        log.info("==> UserSyncEsConsumer 消费消息, userId: {}, tag: {}", body, tag);

        Long userId = Long.valueOf(body);
        if (Objects.equals(tag, MQConstants.TAG_USER_REBUILD)) {
            esSyncService.rebuildUser(userId);
        } else if (Objects.equals(tag, MQConstants.TAG_USER_REBUILD_AND_NOTES)) {
            esSyncService.rebuildUserAndNotes(userId);
        } else {
            log.warn("==> UserSyncEsConsumer 未知 tag: {}", tag);
        }
    }
}
