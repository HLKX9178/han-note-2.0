package com.hanserwei.count.consumer;

import com.hanserwei.count.constant.MQConstants;
import com.hanserwei.count.constant.RedisKeyConstants;
import com.hanserwei.count.model.dto.AggregationCountNoteMqDTO;
import com.hanserwei.count.model.dto.CountLikeUnlikeNoteMqDTO;
import com.hanserwei.count.util.NoteLikeCountAggregator;
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
 * 计数：笔记点赞数消费者（高并发，Reactor 聚合写）.
 *
 * <p>爆款笔记短时间涌入大量点赞，用 {@code bufferTimeout(1000, 1s)} 聚合：满 1000 条或满 1s
 * 触发一次，按笔记 ID 分组净算增量后，同时 {@code HINCRBY} 笔记维度（被点赞数）与用户维度
 * （发布者获赞数）——均仅当 key 存在，再转发落库 MQ。聚合、并发安全、优雅关闭与粉丝数消费者一致。
 *
 * @author hanserwei
 * @date 2026/07/11
 * @since 0.0.1
 */
@Slf4j
@Component
@RequiredArgsConstructor
@RocketMQMessageListener(
        consumerGroup = MQConstants.GROUP_COUNT_NOTE_LIKE,
        topic = MQConstants.TOPIC_COUNT_NOTE_LIKE)
public class CountNoteLikeConsumer implements RocketMQListener<String> {

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
        log.info("## CountNoteLikeConsumer 关闭：flush 聚合缓冲区剩余消息");
        sink.emitComplete(Sinks.EmitFailureHandler.busyLooping(Duration.ofSeconds(1)));
        if (subscription != null) {
            subscription.dispose();
        }
    }

    @Override
    public void onMessage(String body) {
        // RocketMQ 多线程回调；Sinks emit 非并发安全，用 busyLooping 处理并发竞争
        sink.emitNext(body, Sinks.EmitFailureHandler.busyLooping(Duration.ofSeconds(1)));
    }

    /**
     * 处理聚合批次：整体 try/catch 吞异常，避免 onError 终止 bufferTimeout 订阅。
     */
    private void consumeBatch(List<String> bodyList) {
        try {
            doConsumeBatch(bodyList);
        } catch (Exception e) {
            log.error("## 聚合笔记点赞数批处理失败（已吞掉，避免终止订阅）, size: {}", bodyList.size(), e);
        }
    }

    private void doConsumeBatch(List<String> bodyList) {
        log.info("## 聚合笔记点赞数消息, size: {}", bodyList.size());

        List<CountLikeUnlikeNoteMqDTO> dtoList = bodyList.stream()
                .map(body -> JsonUtils.parseObject(body, CountLikeUnlikeNoteMqDTO.class))
                .filter(Objects::nonNull)
                .toList();

        List<AggregationCountNoteMqDTO> countList = NoteLikeCountAggregator.aggregate(dtoList);
        if (countList.isEmpty()) {
            return;
        }
        log.info("## 聚合后的笔记点赞数计数: {}", JsonUtils.toJsonString(countList));

        // 更新 Redis（仅当 Hash key 已存在）
        countList.forEach(item -> {
            // 笔记维度：被点赞数
            String noteKey = RedisKeyConstants.buildCountNoteKey(item.getNoteId());
            if (Boolean.TRUE.equals(redisTemplate.hasKey(noteKey))) {
                redisTemplate.opsForHash().increment(noteKey, RedisKeyConstants.FIELD_LIKE_TOTAL, item.getCount());
            }
            // 用户维度：发布者获赞数
            Long creatorId = item.getCreatorId();
            if (Objects.nonNull(creatorId)) {
                String userKey = RedisKeyConstants.buildCountUserKey(creatorId);
                if (Boolean.TRUE.equals(redisTemplate.hasKey(userKey))) {
                    redisTemplate.opsForHash().increment(userKey, RedisKeyConstants.FIELD_LIKE_TOTAL, item.getCount());
                }
            }
        });

        // 转发落库 MQ，payload 为聚合后的 List JSON
        Message<String> message = MessageBuilder.withPayload(JsonUtils.toJsonString(countList)).build();
        rocketMQTemplate.asyncSend(MQConstants.TOPIC_COUNT_NOTE_LIKE_2_DB, message, new SendCallback() {
            @Override
            public void onSuccess(SendResult sendResult) {
                log.info("==> 【计数服务：笔记点赞数入库】MQ 发送成功, SendResult: {}", sendResult);
            }

            @Override
            public void onException(Throwable throwable) {
                log.error("==> 【计数服务：笔记点赞数入库】MQ 发送异常: ", throwable);
            }
        });
    }
}
