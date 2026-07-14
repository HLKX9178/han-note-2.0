package com.hanserwei.comment.retry;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.client.producer.SendCallback;
import org.apache.rocketmq.client.producer.SendResult;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.retry.RetryCallback;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
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

    private final RocketMQTemplate rocketMQTemplate;
    private final RetryTemplate retryTemplate;
    private final ThreadPoolTaskExecutor taskExecutor;

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
                handleRetry(topic, message);
            }
        });
    }

    /**
     * 失败后异步指数退避重试.
     */
    private void handleRetry(String topic, Message<String> message) {
        taskExecutor.submit(() -> {
            try {
                retryTemplate.execute((RetryCallback<Void, RuntimeException>) context -> {
                    log.info("==> 重试发送 MQ, 第 {} 次, 时间: {}", context.getRetryCount() + 1, LocalDateTime.now());
                    rocketMQTemplate.syncSend(topic, message);
                    return null;
                });
            } catch (Exception e) {
                fallback(e, topic, message.getPayload());
            }
        });
    }

    /**
     * 兜底：多次重试仍失败.
     *
     * <p>TODO 后续实现：将失败消息落库，定时任务扫表重发，成功后物理删除。本批仅记录错误日志。
     */
    private void fallback(Exception e, String topic, String bodyJson) {
        log.error("==> 多次发送失败, 进入兜底方案（暂仅记录）, Topic: {}, body: {}", topic, bodyJson, e);
        // TODO: 失败消息落库 + 定时补偿重发
    }
}
