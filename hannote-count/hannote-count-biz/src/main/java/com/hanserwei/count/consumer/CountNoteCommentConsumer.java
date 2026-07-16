package com.hanserwei.count.consumer;

import com.hanserwei.count.constant.MQConstants;
import com.hanserwei.count.constant.RedisKeyConstants;
import com.hanserwei.count.model.dto.AggregationCountNoteMqDTO;
import com.hanserwei.count.model.dto.CommentCountChangedMqDTO;
import com.hanserwei.count.util.NoteCommentCountAggregator;
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
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Component;
import reactor.core.Disposable;
import reactor.core.publisher.Sinks;

import java.time.Duration;
import java.util.List;
import java.util.Objects;

/**
 * 笔记评论总数高并发聚合消费者.
 *
 * <p>消费评论侧发来的评论数变更源事件（{@link MQConstants#TOPIC_COMMENT_COUNT_CHANGED}），
 * 采用「攒批 + 聚合」削峰：消息不逐条处理，而是先投递到 Reactor {@link Sinks} 缓冲，
 * 由 {@code bufferTimeout(1000, 1s)} 按「满 1000 条或满 1 秒」触发一批，交给
 * {@link NoteCommentCountAggregator} 按 noteId 汇总有符号增量后，仅对净增量非 0 的笔记：
 * 先增量刷新 Redis 计数缓存（缓存不存在则跳过，读时再回源），再异步转发落库 Topic
 * （{@link MQConstants#TOPIC_COUNT_NOTE_COMMENT_2_DB}）由
 * {@link CountNoteComment2DBConsumer} 落库，形成最终一致。
 *
 * @author hanserwei
 * @date 2026/07/15
 * @since 0.0.1
 */
@Slf4j
@Component
@RequiredArgsConstructor
@RocketMQMessageListener(
        consumerGroup = MQConstants.GROUP_COUNT_NOTE_COMMENT,
        topic = MQConstants.TOPIC_COMMENT_COUNT_CHANGED)
public class CountNoteCommentConsumer implements RocketMQListener<String> {

    private final RedisTemplate<String, Object> redisTemplate;
    private final RocketMQTemplate rocketMQTemplate;
    private final Sinks.Many<String> sink = Sinks.many().unicast().onBackpressureBuffer();
    private Disposable subscription;

    /**
     * 订阅缓冲流，按「满 1000 条或满 1 秒」攒批触发 {@link #consumeBatch}.
     */
    @PostConstruct
    public void init() {
        subscription = sink.asFlux()
                .bufferTimeout(1000, Duration.ofSeconds(1))
                .subscribe(this::consumeBatch);
    }

    /**
     * 应用关闭时优雅收尾：完结缓冲流并释放订阅，避免残留消息丢失.
     */
    @PreDestroy
    public void shutdown() {
        sink.emitComplete(Sinks.EmitFailureHandler.busyLooping(Duration.ofSeconds(1)));
        if (subscription != null) {
            subscription.dispose();
        }
    }

    /**
     * 消费入口：不逐条处理，仅把消息体投递进缓冲流，交由攒批逻辑聚合.
     *
     * @param body 评论数变更源事件 JSON
     */
    @Override
    public void onMessage(String body) {
        sink.emitNext(body, Sinks.EmitFailureHandler.busyLooping(Duration.ofSeconds(1)));
    }

    /**
     * 处理一批攒好的消息：聚合增量 → 刷新 Redis 缓存 → 异步转发落库.
     *
     * <p>整个方法体用 try 包裹并吞掉异常：聚合/发送失败不应终止 Reactor 订阅
     * （订阅一旦 onError 结束将不再消费后续批次），异常仅以 {@code ##} 前缀告警。
     *
     * @param bodies 本批次原始消息体列表
     */
    private void consumeBatch(List<String> bodies) {
        try {
            // 1. 反序列化本批消息，剔除解析失败的空值
            List<CommentCountChangedMqDTO> source = bodies.stream()
                    .map(body -> JsonUtils.parseObject(body, CommentCountChangedMqDTO.class))
                    .filter(Objects::nonNull)
                    .toList();
            // 2. 按 noteId 聚合有符号增量，净增量为 0 的项已被丢弃
            List<AggregationCountNoteMqDTO> counts = NoteCommentCountAggregator.aggregate(source);
            if (counts.isEmpty()) {
                return;
            }
            // 3. 增量刷新 Redis 计数缓存；缓存不存在则跳过，避免写入残缺 Hash（读时再回源重建）
            counts.forEach(item -> {
                String key = RedisKeyConstants.buildCountNoteKey(item.getNoteId());
                if (Boolean.TRUE.equals(redisTemplate.hasKey(key))) {
                    redisTemplate.opsForHash().increment(
                            key, RedisKeyConstants.FIELD_COMMENT_TOTAL, item.getCount());
                }
            });
            // 4. 异步转发聚合结果到落库 Topic，由 CountNoteComment2DBConsumer 落库
            rocketMQTemplate.asyncSend(MQConstants.TOPIC_COUNT_NOTE_COMMENT_2_DB,
                    MessageBuilder.withPayload(JsonUtils.toJsonString(counts)).build(), new SendCallback() {
                        @Override
                        public void onSuccess(SendResult sendResult) {
                            log.info("==> 【计数服务：笔记评论数入库】MQ 发送成功, SendResult: {}", sendResult);
                        }

                        @Override
                        public void onException(Throwable throwable) {
                            log.error("==> 【计数服务：笔记评论数入库】MQ 发送异常", throwable);
                        }
                    });
        } catch (Exception e) {
            log.error("## 聚合笔记评论总数失败（已吞掉，避免终止订阅）, size: {}", bodies.size(), e);
        }
    }
}
