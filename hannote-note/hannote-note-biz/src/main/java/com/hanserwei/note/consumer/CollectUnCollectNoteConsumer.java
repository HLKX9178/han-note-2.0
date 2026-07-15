package com.hanserwei.note.consumer;

import cn.hutool.core.collection.CollUtil;
import com.google.common.util.concurrent.RateLimiter;
import com.hanserwei.framework.common.util.InteractionMergeSupport;
import com.hanserwei.framework.common.util.JsonUtils;
import com.hanserwei.note.constant.MQConstants;
import com.hanserwei.note.domain.dataobject.NoteCollectionDO;
import com.hanserwei.note.domain.mapper.NoteCollectionDOMapper;
import com.hanserwei.note.model.dto.CollectUnCollectNoteMqDTO;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.client.consumer.DefaultMQPushConsumer;
import org.apache.rocketmq.client.consumer.listener.ConsumeOrderlyStatus;
import org.apache.rocketmq.client.consumer.listener.MessageListenerOrderly;
import org.apache.rocketmq.client.exception.MQClientException;
import org.apache.rocketmq.common.consumer.ConsumeFromWhere;
import org.apache.rocketmq.remoting.protocol.heartbeat.MessageModel;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Objects;

/**
 * 笔记收藏 / 取消收藏 MQ 消费者（rocketmq-client 原生批量顺序消费）.
 *
 * <p>批量顺序消费 {@link MQConstants#TOPIC_COLLECT_UNCOLLECT}：令牌桶削峰后，对一批消息按
 * {@code (userId, noteId)} 合并为最终状态（{@link InteractionMergeSupport}，取批次内最后一条），
 * 将合并后的最终操作批量 upsert 到 {@code t_note_collection}（status 位翻转，1 收藏 / 0 取消）。
 *
 * <p>顺序性由生产端按 hashKey 有序发送保证。计数已改由计数服务并行直消费源 Topic
 * {@code TOPIC_COLLECT_UNCOLLECT}，本消费者不再转发计数 MQ。逻辑与点赞消费者一致。
 *
 * @author hanserwei
 * @date 2026/07/14
 * @since 0.0.1
 */
@Slf4j
@Component
public class CollectUnCollectNoteConsumer {

    private final String namesrvAddr;
    private final NoteCollectionDOMapper noteCollectionDOMapper;

    private DefaultMQPushConsumer consumer;

    /** 令牌桶削峰：每秒 5000 个令牌 */
    private final RateLimiter rateLimiter = RateLimiter.create(5000);

    public CollectUnCollectNoteConsumer(@Value("${rocketmq.name-server}") String namesrvAddr,
                                        NoteCollectionDOMapper noteCollectionDOMapper) {
        this.namesrvAddr = namesrvAddr;
        this.noteCollectionDOMapper = noteCollectionDOMapper;
    }

    @PostConstruct
    public void init() throws MQClientException {
        consumer = new DefaultMQPushConsumer(MQConstants.GROUP_COLLECT_UNCOLLECT);
        consumer.setNamesrvAddr(namesrvAddr);
        consumer.subscribe(MQConstants.TOPIC_COLLECT_UNCOLLECT, "*");
        consumer.setConsumeFromWhere(ConsumeFromWhere.CONSUME_FROM_LAST_OFFSET);
        consumer.setMessageModel(MessageModel.CLUSTERING);
        consumer.setConsumeMessageBatchMaxSize(30);
        consumer.setPullInterval(1000);
        consumer.setMaxReconsumeTimes(3);

        consumer.registerMessageListener((MessageListenerOrderly) (msgs, context) -> {
            log.info("==> 【笔记收藏/取消收藏】本批次消息大小: {}", msgs.size());
            try {
                // 流量削峰：按条消耗令牌（批量消费下按批消耗会使实际吞吐放大 batchSize 倍）
                rateLimiter.acquire(msgs.size());

                // 消息体 -> DTO 列表
                List<CollectUnCollectNoteMqDTO> dtos = msgs.stream()
                        .map(m -> JsonUtils.parseObject(new String(m.getBody(), StandardCharsets.UTF_8), CollectUnCollectNoteMqDTO.class))
                        .filter(Objects::nonNull)
                        .toList();

                // 内存合并：同 (userId, noteId) 取批次内最后一条作为最终状态
                List<CollectUnCollectNoteMqDTO> merged = InteractionMergeSupport.mergeByLastOp(
                        dtos, CollectUnCollectNoteMqDTO::getUserId, CollectUnCollectNoteMqDTO::getNoteId);

                if (CollUtil.isNotEmpty(merged)) {
                    List<NoteCollectionDO> dos = merged.stream()
                            .map(d -> NoteCollectionDO.builder()
                                    .userId(d.getUserId())
                                    .noteId(d.getNoteId())
                                    .createTime(d.getCreateTime())
                                    .status(d.getType())
                                    .build())
                            .toList();
                    noteCollectionDOMapper.batchInsertOrUpdate(dos);
                }

                return ConsumeOrderlyStatus.SUCCESS;
            } catch (Exception e) {
                log.error("==> 【笔记收藏/取消收藏】批量消费失败，挂起当前队列稍后重试: ", e);
                return ConsumeOrderlyStatus.SUSPEND_CURRENT_QUEUE_A_MOMENT;
            }
        });

        consumer.start();
        log.info("## CollectUnCollectNoteConsumer 启动完成，批量顺序消费 {}", MQConstants.TOPIC_COLLECT_UNCOLLECT);
    }

    @PreDestroy
    public void destroy() {
        if (Objects.nonNull(consumer)) {
            try {
                consumer.shutdown();
            } catch (Exception e) {
                log.error("==> CollectUnCollectNoteConsumer 关闭异常: ", e);
            }
        }
    }
}
