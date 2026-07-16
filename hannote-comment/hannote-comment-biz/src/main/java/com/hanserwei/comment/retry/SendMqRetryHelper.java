package com.hanserwei.comment.retry;

import com.hanserwei.comment.domain.dataobject.MqSendFailDO;
import com.hanserwei.comment.domain.mapper.MqSendFailDOMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.client.producer.SendCallback;
import org.apache.rocketmq.client.producer.SendResult;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.retry.RetryCallback;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * 发送 MQ 可靠性工具：异步发送，失败后异步指数退避重试，多次失败进入兜底.
 *
 * @author hanserwei
 * @date 2026/07/14
 * @since 0.0.1
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SendMqRetryHelper {

    /** RocketMQ 模板：异步首发 + 重试时同步重发 */
    private final RocketMQTemplate rocketMQTemplate;
    /** 重试模板：封装指数退避重试策略 */
    private final RetryTemplate retryTemplate;
    /** 异步执行器（虚拟线程，见 ThreadPoolConfig）：承载重试任务的退避 sleep + 阻塞发送 */
    private final AsyncTaskExecutor taskExecutor;
    /** 兜底表 Mapper：多次重试仍失败时落库待定时补偿 */
    private final MqSendFailDOMapper mqSendFailDOMapper;

    /**
     * 异步发送 MQ.
     *
     * @param topic 主题
     * @param body  消息体 JSON
     */
    public void asyncSend(String topic, String body) {
        log.info("==> 开始异步发送 MQ, Topic: {}, Body: {}", topic, body);
        Message<String> message = MessageBuilder.withPayload(body).build();

        rocketMQTemplate.asyncSend(topic, message, new SendCallback() {
            @Override
            public void onSuccess(SendResult sendResult) {
                log.info("==> 【评论发布】MQ 发送成功, SendResult: {}", sendResult);
            }

            @Override
            public void onException(Throwable throwable) {
                log.error("==> 【评论发布】MQ 发送异常, 进入重试: ", throwable);
                handleRetry(topic, message, false, "");
            }
        });
    }

    /**
     * 异步发送顺序 MQ，失败补偿时保留原 hashKey.
     *
     * @param destination Topic:Tag
     * @param body 消息体 JSON
     * @param hashKey 顺序分片键
     */
    public void asyncSendOrderly(String destination, String body, String hashKey) {
        log.info("==> 开始异步发送顺序 MQ, destination: {}, hashKey: {}", destination, hashKey);
        Message<String> message = MessageBuilder.withPayload(body).build();
        rocketMQTemplate.asyncSendOrderly(destination, message, hashKey, new SendCallback() {
            @Override
            public void onSuccess(SendResult sendResult) {
                log.info("==> 顺序 MQ 发送成功, destination: {}, SendResult: {}", destination, sendResult);
            }

            @Override
            public void onException(Throwable throwable) {
                log.error("==> 顺序 MQ 发送异常, destination: {}, 进入重试", destination, throwable);
                handleRetry(destination, message, true, hashKey);
            }
        });
    }

    /**
     * 失败后异步指数退避重试.
     */
    private void handleRetry(String topic, Message<String> message, boolean orderly, String hashKey) {
        taskExecutor.submit(() -> {
            try {
                retryTemplate.execute((RetryCallback<Void, RuntimeException>) context -> {
                    log.info("==> 重试发送 MQ, 第 {} 次, 时间: {}", context.getRetryCount() + 1, LocalDateTime.now());
                    if (orderly) {
                        rocketMQTemplate.syncSendOrderly(topic, message, hashKey);
                    } else {
                        rocketMQTemplate.syncSend(topic, message);
                    }
                    return null;
                });
            } catch (Exception e) {
                fallback(e, topic, message.getPayload(), orderly, hashKey);
            }
        });
    }

    /**
     * 兜底：多次重试仍失败，将消息落库 {@code t_mq_send_fail}.
     *
     * <p>由 PowerJob 定时任务（{@code MqResendProcessor}）扫表重发，成功后物理删除，做到 MQ 最终发出。
     *
     * <p>落库本身也可能失败（如 MQ 与 DB 同时故障）。此处必须自行捕获：调用方在虚拟线程中执行，
     * 异常外溢将被静默丢弃，消息彻底消失且无迹可循。故失败时以 error 日志打印完整消息体，
     * 保证至少可从日志人工补偿。
     */
    private void fallback(Exception e, String topic, String bodyJson, boolean orderly, String hashKey) {
        log.error("==> 多次发送失败, 进入兜底方案（落库待补偿重发）, Topic: {}, body: {}", topic, bodyJson, e);
        LocalDateTime now = LocalDateTime.now();
        try {
            mqSendFailDOMapper.insert(MqSendFailDO.builder()
                    .topic(topic)
                    .body(bodyJson)
                    .orderly(orderly)
                    .hashKey(hashKey == null ? "" : hashKey)
                    .retryCount(0)
                    .nextRetryTime(now)
                    .status(0)
                    .createTime(now)
                    .updateTime(now)
                    .build());
        } catch (Exception dbEx) {
            log.error("==> 兜底落库失败，消息仅存于本日志，需人工补偿! Topic: {}, body: {}", topic, bodyJson, dbEx);
        }
    }
}
