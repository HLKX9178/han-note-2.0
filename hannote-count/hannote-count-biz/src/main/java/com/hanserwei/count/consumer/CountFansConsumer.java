package com.hanserwei.count.consumer;

import com.hanserwei.count.constant.MQConstants;
import com.hanserwei.count.constant.RedisKeyConstants;
import com.hanserwei.count.model.dto.CountFollowUnfollowMqDTO;
import com.hanserwei.count.util.FansCountAggregator;
import com.hanserwei.count.util.FollowUnfollowSourceParser;
import com.hanserwei.framework.common.util.JsonUtils;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.client.producer.SendCallback;
import org.apache.rocketmq.client.producer.SendResult;
import org.apache.rocketmq.common.message.MessageExt;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Component;
import reactor.core.Disposable;
import reactor.core.publisher.Sinks;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * 计数：粉丝数消费者（高并发，Reactor 聚合写）.
 *
 * <p>粉丝数为高并发写场景（爆款用户短时间涌入大量关注）。用 Project Reactor 的
 * {@code bufferTimeout(1000, 1s)} 聚合：满 1000 条或满 1s 触发一次，按目标用户分组净算
 * 增量后 {@code HINCRBY}（仅当 key 存在），再转发落库 MQ。
 *
 * <p>RocketMQ 多线程回调 {@link #onMessage}，而 Reactor {@code Sinks} emit 非并发安全，
 * 故用 {@code emitNext + busyLooping} 处理并发竞争。
 *
 * <p>已改为**并行直消费源 Topic** {@link MQConstants#TOPIC_FOLLOW_OR_UNFOLLOW}（与 relation 落库
 * 消费者并行）。源体按 Tag 经 {@link FollowUnfollowSourceParser} 归一化为
 * {@link CountFollowUnfollowMqDTO} 后再入聚合队列，下游聚合/落库逻辑不变。幂等门移除的短时计数
 * 漂移由 hannote-data-align 日次纠偏自愈。
 *
 * @author hanserwei
 * @date 2026/07/11
 * @since 0.0.1
 */
@Slf4j
@Component
@RequiredArgsConstructor
@RocketMQMessageListener(
        consumerGroup = MQConstants.GROUP_COUNT_FANS,
        topic = MQConstants.TOPIC_FOLLOW_OR_UNFOLLOW)
public class CountFansConsumer implements RocketMQListener<MessageExt> {

    private final RedisTemplate<String, Object> redisTemplate;
    private final RocketMQTemplate rocketMQTemplate;

    /** 聚合队列：满 1000 条或满 1s 触发一次批处理 */
    private final Sinks.Many<CountFollowUnfollowMqDTO> sink = Sinks.many().unicast().onBackpressureBuffer();

    /** bufferTimeout 订阅句柄，用于优雅关闭时释放 */
    private Disposable subscription;

    @PostConstruct
    public void init() {
        subscription = sink.asFlux()
                .bufferTimeout(1000, Duration.ofSeconds(1))
                .subscribe(this::consumeBatch);
    }

    /**
     * 优雅关闭：先补发完成信号，令 {@code bufferTimeout} 把缓冲区里尚未满 1000 条/未到 1s 的
     * 剩余消息同步 flush 到 {@link #consumeBatch}（写 Redis + 转发落库 MQ），再释放订阅。
     *
     * <p>unicast sink 的完成信号在调用线程上同步传播，因此本方法返回前最后一批已处理完毕，
     * 避免正常停机（重启/发版）时丢失缓冲中的粉丝计数。注意：进程硬崩仍会丢失缓冲区，
     * 需由计数对账兜底任务补偿（本期范围外）。
     */
    @PreDestroy
    public void shutdown() {
        log.info("## CountFansConsumer 关闭：flush 聚合缓冲区剩余消息");
        // 补发完成信号，触发 bufferTimeout 同步 flush 最后一批
        sink.emitComplete(Sinks.EmitFailureHandler.busyLooping(Duration.ofSeconds(1)));
        if (subscription != null) {
            subscription.dispose();
        }
    }

    @Override
    public void onMessage(MessageExt message) {
        String body = new String(message.getBody(), StandardCharsets.UTF_8);
        // 源体按 Tag 归一化为计数 DTO 后直接入聚合队列（sink 持有 DTO，无需再序列化往返）
        CountFollowUnfollowMqDTO dto = FollowUnfollowSourceParser.parse(message.getTags(), body);
        if (Objects.isNull(dto)) {
            return;
        }
        // RocketMQ 多线程回调；Sinks emit 非并发安全，用 busyLooping 处理并发竞争
        sink.emitNext(dto, Sinks.EmitFailureHandler.busyLooping(Duration.ofSeconds(1)));
    }

    /**
     * 处理聚合批次：分组净算 → 写 Redis → 转发落库 MQ。
     *
     * <p>整体包裹 try/catch 吞掉异常：本方法作为 {@code bufferTimeout} 订阅的 onNext 回调，
     * 若异常外溢会触发 Flux 的 onError 使订阅永久终止，此后所有粉丝数消息将无人处理，
     * 只能重启进程恢复。故此处宁可丢失本批（由计数对账兜底），也不能让管道断流。
     *
     * @param dtoList 本批次归一化后的计数 DTO
     */
    private void consumeBatch(List<CountFollowUnfollowMqDTO> dtoList) {
        try {
            doConsumeBatch(dtoList);
        } catch (Exception e) {
            log.error("## 聚合粉丝数批处理失败（已吞掉，避免终止订阅）, size: {}", dtoList.size(), e);
        }
    }

    private void doConsumeBatch(List<CountFollowUnfollowMqDTO> dtoList) {
        log.info("## 聚合粉丝数消息, size: {}", dtoList.size());

        // 按目标用户分组净算增量
        Map<Long, Integer> countMap = FansCountAggregator.aggregate(dtoList);
        if (countMap.isEmpty()) {
            return;
        }
        log.info("## 聚合后的粉丝数计数: {}", JsonUtils.toJsonString(countMap));

        // 更新 Redis（仅当 Hash key 已存在）
        countMap.forEach((targetUserId, delta) -> {
            String redisKey = RedisKeyConstants.buildCountUserKey(targetUserId);
            if (Boolean.TRUE.equals(redisTemplate.hasKey(redisKey))) {
                redisTemplate.opsForHash().increment(redisKey, RedisKeyConstants.FIELD_FANS_TOTAL, delta);
            }
        });

        // 转发落库 MQ，payload 为聚合后的 countMap JSON
        Message<String> message = MessageBuilder.withPayload(JsonUtils.toJsonString(countMap)).build();
        rocketMQTemplate.asyncSend(MQConstants.TOPIC_COUNT_FANS_2_DB, message, new SendCallback() {
            @Override
            public void onSuccess(SendResult sendResult) {
                log.info("==> 【计数服务：粉丝数入库】MQ 发送成功, SendResult: {}", sendResult);
            }

            @Override
            public void onException(Throwable throwable) {
                log.error("==> 【计数服务：粉丝数入库】MQ 发送异常: ", throwable);
            }
        });
    }
}
