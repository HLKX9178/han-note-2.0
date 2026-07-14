package com.hanserwei.note.consumer;

import cn.hutool.core.collection.CollUtil;
import com.google.common.util.concurrent.RateLimiter;
import com.hanserwei.framework.common.util.JsonUtils;
import com.hanserwei.note.constant.MQConstants;
import com.hanserwei.note.consumer.support.InteractionMergeSupport;
import com.hanserwei.note.domain.dataobject.NoteLikeDO;
import com.hanserwei.note.domain.mapper.NoteLikeDOMapper;
import com.hanserwei.note.model.dto.LikeUnlikeNoteMqDTO;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
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
 * 笔记点赞 / 取消点赞 MQ 消费者（rocketmq-client 原生批量顺序消费）.
 *
 * <p>批量顺序消费 {@link MQConstants#TOPIC_LIKE_UNLIKE}：令牌桶削峰后，对一批消息按
 * {@code (userId, noteId)} 做奇偶抵消合并（{@link InteractionMergeSupport}），将合并后的
 * 最终操作批量 upsert 到 {@code t_note_like}（status 位翻转，1 点赞 / 0 取消）。
 *
 * <p>顺序性由生产端按 {@code userId} hashKey 有序发送保证（同一用户操作落同一队列）。
 *
 * <p>计数已改由计数服务并行直消费源 Topic {@code TOPIC_LIKE_UNLIKE}，本消费者不再转发计数 MQ。
 *
 * @author hanserwei
 * @date 2026/07/14
 * @since 0.0.1
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class LikeUnlikeNoteConsumer {

    @Value("${rocketmq.name-server}")
    private String namesrvAddr;

    private final NoteLikeDOMapper noteLikeDOMapper;

    private DefaultMQPushConsumer consumer;

    /** 令牌桶削峰：每秒 5000 个令牌，以数据库可承受速率消费 */
    private final RateLimiter rateLimiter = RateLimiter.create(5000);

    @PostConstruct
    public void init() throws MQClientException {
        consumer = new DefaultMQPushConsumer(MQConstants.GROUP_LIKE_UNLIKE);
        consumer.setNamesrvAddr(namesrvAddr);
        consumer.subscribe(MQConstants.TOPIC_LIKE_UNLIKE, "*");
        consumer.setConsumeFromWhere(ConsumeFromWhere.CONSUME_FROM_LAST_OFFSET);
        consumer.setMessageModel(MessageModel.CLUSTERING);
        consumer.setConsumeMessageBatchMaxSize(30);
        consumer.setPullInterval(1000);
        consumer.setMaxReconsumeTimes(3);

        consumer.registerMessageListener((MessageListenerOrderly) (msgs, context) -> {
            log.info("==> 【笔记点赞/取消点赞】本批次消息大小: {}", msgs.size());
            try {
                // 流量削峰
                rateLimiter.acquire();

                // 消息体 -> DTO 列表
                List<LikeUnlikeNoteMqDTO> dtos = msgs.stream()
                        .map(m -> JsonUtils.parseObject(new String(m.getBody(), StandardCharsets.UTF_8), LikeUnlikeNoteMqDTO.class))
                        .filter(Objects::nonNull)
                        .toList();

                // 内存合并：同 (userId, noteId) 偶数次抵消、奇数次取最后一次
                List<LikeUnlikeNoteMqDTO> merged = InteractionMergeSupport.mergeByLastOp(
                        dtos, LikeUnlikeNoteMqDTO::getUserId, LikeUnlikeNoteMqDTO::getNoteId);

                if (CollUtil.isNotEmpty(merged)) {
                    List<NoteLikeDO> dos = merged.stream()
                            .map(d -> NoteLikeDO.builder()
                                    .userId(d.getUserId())
                                    .noteId(d.getNoteId())
                                    .createTime(d.getCreateTime())
                                    .status(d.getType())
                                    .build())
                            .toList();
                    noteLikeDOMapper.batchInsertOrUpdate(dos);
                }

                return ConsumeOrderlyStatus.SUCCESS;
            } catch (Exception e) {
                log.error("==> 【笔记点赞/取消点赞】批量消费失败，挂起当前队列稍后重试: ", e);
                return ConsumeOrderlyStatus.SUSPEND_CURRENT_QUEUE_A_MOMENT;
            }
        });

        consumer.start();
        log.info("## LikeUnlikeNoteConsumer 启动完成，批量顺序消费 {}", MQConstants.TOPIC_LIKE_UNLIKE);
    }

    @PreDestroy
    public void destroy() {
        if (Objects.nonNull(consumer)) {
            try {
                consumer.shutdown();
            } catch (Exception e) {
                log.error("==> LikeUnlikeNoteConsumer 关闭异常: ", e);
            }
        }
    }
}
