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

    @PostConstruct
    public void init() {
        subscription = sink.asFlux()
                .bufferTimeout(1000, Duration.ofSeconds(1))
                .subscribe(this::consumeBatch);
    }

    @PreDestroy
    public void shutdown() {
        sink.emitComplete(Sinks.EmitFailureHandler.busyLooping(Duration.ofSeconds(1)));
        if (subscription != null) {
            subscription.dispose();
        }
    }

    @Override
    public void onMessage(String body) {
        sink.emitNext(body, Sinks.EmitFailureHandler.busyLooping(Duration.ofSeconds(1)));
    }

    private void consumeBatch(List<String> bodies) {
        try {
            List<CommentCountChangedMqDTO> source = bodies.stream()
                    .map(body -> JsonUtils.parseObject(body, CommentCountChangedMqDTO.class))
                    .filter(Objects::nonNull)
                    .toList();
            List<AggregationCountNoteMqDTO> counts = NoteCommentCountAggregator.aggregate(source);
            if (counts.isEmpty()) {
                return;
            }
            counts.forEach(item -> {
                String key = RedisKeyConstants.buildCountNoteKey(item.getNoteId());
                if (Boolean.TRUE.equals(redisTemplate.hasKey(key))) {
                    redisTemplate.opsForHash().increment(
                            key, RedisKeyConstants.FIELD_COMMENT_TOTAL, item.getCount());
                }
            });
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
