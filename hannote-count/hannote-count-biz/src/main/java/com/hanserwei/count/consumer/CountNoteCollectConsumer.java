package com.hanserwei.count.consumer;

import com.hanserwei.count.constant.MQConstants;
import com.hanserwei.count.constant.RedisKeyConstants;
import com.hanserwei.count.model.dto.AggregationCountNoteMqDTO;
import com.hanserwei.count.model.dto.CountCollectUnCollectNoteMqDTO;
import com.hanserwei.count.util.NoteCollectCountAggregator;
import com.hanserwei.framework.common.util.JsonUtils;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.client.producer.SendCallback;
import org.apache.rocketmq.client.producer.SendResult;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Component;
import reactor.core.Disposable;
import reactor.core.publisher.Sinks;

import java.time.Duration;
import java.util.List;
import java.util.Objects;

/**
 * 计数：笔记收藏数消费者（高并发，Reactor 聚合写）.
 *
 * <p>与点赞数消费者同构：{@code bufferTimeout(1000, 1s)} 聚合，按笔记 ID 净算增量后同时
 * {@code HINCRBY} 笔记维度（被收藏数）与用户维度（发布者获藏数），再转发落库 MQ。
 *
 * <p>本消费者已改为**并行直消费源 Topic** {@link MQConstants#TOPIC_COLLECT_UNCOLLECT}（与 note 收藏
 * 落库消费者并行）。幂等门移除的短时计数漂移由 hannote-data-align 日次纠偏自愈；源体
 * {@code CountCollectUnCollectNoteMqDTO} 字段与源 {@code CollectUnCollectNoteMqDTO} 一致，可直接解析。
 *
 * @author hanserwei
 * @date 2026/07/11
 * @since 0.0.1
 */
@Slf4j
@Component
@RequiredArgsConstructor
@RocketMQMessageListener(
        consumerGroup = MQConstants.GROUP_COUNT_NOTE_COLLECT,
        topic = MQConstants.TOPIC_COLLECT_UNCOLLECT)
public class CountNoteCollectConsumer implements RocketMQListener<String> {

    private final RedisTemplate<String, Object> redisTemplate;
    private final RocketMQTemplate rocketMQTemplate;

    /** 聚合队列：满 1000 条或满 1s 触发一次批处理 */
    private final Sinks.Many<String> sink = Sinks.many().unicast().onBackpressureBuffer();

    /** bufferTimeout 订阅句柄，用于优雅关闭时释放 */
    private Disposable subscription;

    @PostConstruct
    public void init() {
        subscription = sink.asFlux()
                .bufferTimeout(1000, Duration.ofSeconds(1))
                .subscribe(this::consumeBatch);
    }

    @PreDestroy
    public void shutdown() {
        log.info("## CountNoteCollectConsumer 关闭：flush 聚合缓冲区剩余消息");
        sink.emitComplete(Sinks.EmitFailureHandler.busyLooping(Duration.ofSeconds(1)));
        if (subscription != null) {
            subscription.dispose();
        }
    }

    @Override
    public void onMessage(String body) {
        sink.emitNext(body, Sinks.EmitFailureHandler.busyLooping(Duration.ofSeconds(1)));
    }

    private void consumeBatch(List<String> bodyList) {
        try {
            doConsumeBatch(bodyList);
        } catch (Exception e) {
            log.error("## 聚合笔记收藏数批处理失败（已吞掉，避免终止订阅）, size: {}", bodyList.size(), e);
        }
    }

    private void doConsumeBatch(List<String> bodyList) {
        log.info("## 聚合笔记收藏数消息, size: {}", bodyList.size());

        List<CountCollectUnCollectNoteMqDTO> dtoList = bodyList.stream()
                .map(body -> JsonUtils.parseObject(body, CountCollectUnCollectNoteMqDTO.class))
                .filter(Objects::nonNull)
                .toList();

        List<AggregationCountNoteMqDTO> countList = NoteCollectCountAggregator.aggregate(dtoList);
        if (countList.isEmpty()) {
            return;
        }
        log.info("## 聚合后的笔记收藏数计数: {}", JsonUtils.toJsonString(countList));

        countList.forEach(item -> {
            // 笔记维度：被收藏数
            String noteKey = RedisKeyConstants.buildCountNoteKey(item.getNoteId());
            if (Boolean.TRUE.equals(redisTemplate.hasKey(noteKey))) {
                redisTemplate.opsForHash().increment(noteKey, RedisKeyConstants.FIELD_COLLECT_TOTAL, item.getCount());
            }
            // 用户维度：发布者获藏数
            Long creatorId = item.getCreatorId();
            if (Objects.nonNull(creatorId)) {
                String userKey = RedisKeyConstants.buildCountUserKey(creatorId);
                if (Boolean.TRUE.equals(redisTemplate.hasKey(userKey))) {
                    redisTemplate.opsForHash().increment(userKey, RedisKeyConstants.FIELD_COLLECT_TOTAL, item.getCount());
                }
            }
        });

        Message<String> message = MessageBuilder.withPayload(JsonUtils.toJsonString(countList)).build();
        rocketMQTemplate.asyncSend(MQConstants.TOPIC_COUNT_NOTE_COLLECT_2_DB, message, new SendCallback() {
            @Override
            public void onSuccess(SendResult sendResult) {
                log.info("==> 【计数服务：笔记收藏数入库】MQ 发送成功, SendResult: {}", sendResult);
            }

            @Override
            public void onException(Throwable throwable) {
                log.error("==> 【计数服务：笔记收藏数入库】MQ 发送异常: ", throwable);
            }
        });
    }
}
