package com.hanserwei.comment.consumer;

import cn.hutool.core.collection.CollUtil;
import com.hanserwei.comment.assembler.CommentAssembler;
import com.hanserwei.comment.constant.MQConstants;
import com.hanserwei.comment.domain.dataobject.CommentDO;
import com.hanserwei.comment.domain.mapper.CommentDOMapper;
import com.hanserwei.comment.model.bo.CommentBO;
import com.hanserwei.comment.model.dto.PublishCommentMqDTO;
import com.hanserwei.comment.rpc.KeyValueRpcService;
import com.google.common.util.concurrent.RateLimiter;
import com.hanserwei.framework.common.util.JsonUtils;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.client.consumer.DefaultMQPushConsumer;
import org.apache.rocketmq.client.consumer.listener.ConsumeConcurrentlyStatus;
import org.apache.rocketmq.client.consumer.listener.MessageListenerConcurrently;
import org.apache.rocketmq.client.exception.MQClientException;
import org.apache.rocketmq.common.consumer.ConsumeFromWhere;
import org.apache.rocketmq.remoting.protocol.heartbeat.MessageModel;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 评论批量写库消费者.
 *
 * <p>用 rocketmq-client 原生 {@link DefaultMQPushConsumer} 批量消费 {@code PublishCommentTopic}，
 * 令牌桶限流后拼装 {@link CommentBO}，编程式事务内批量写 {@code t_comment}（ON CONFLICT DO NOTHING）
 * 并 RPC 批量写 ScyllaDB 评论正文。
 *
 * @author hanserwei
 * @date 2026/07/14
 * @since 0.0.1
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class Comment2DBConsumer {

    @Value("${rocketmq.name-server}")
    private String namesrvAddr;

    private final CommentDOMapper commentDOMapper;
    private final CommentAssembler commentAssembler;
    private final KeyValueRpcService keyValueRpcService;
    private final TransactionTemplate transactionTemplate;

    private DefaultMQPushConsumer consumer;

    /** 令牌桶：每秒 1000（按数据库承受力调整） */
    private final RateLimiter rateLimiter = RateLimiter.create(1000);

    @Bean(name = "comment2DBConsumer")
    public DefaultMQPushConsumer mqPushConsumer() throws MQClientException {
        String group = "hannote_comment_group_" + MQConstants.TOPIC_PUBLISH_COMMENT;

        consumer = new DefaultMQPushConsumer(group);
        consumer.setNamesrvAddr(namesrvAddr);
        consumer.subscribe(MQConstants.TOPIC_PUBLISH_COMMENT, "*");
        consumer.setConsumeFromWhere(ConsumeFromWhere.CONSUME_FROM_LAST_OFFSET);
        consumer.setMessageModel(MessageModel.CLUSTERING);
        consumer.setConsumeMessageBatchMaxSize(30);

        consumer.registerMessageListener((MessageListenerConcurrently) (msgs, context) -> {
            log.info("==> 评论批量写库，本批次消息大小: {}", msgs.size());
            try {
                // 令牌桶限流
                rateLimiter.acquire();

                // 消息体 -> DTO 集合
                List<PublishCommentMqDTO> dtos = msgs.stream()
                        .map(msg -> JsonUtils.parseObject(new String(msg.getBody(), StandardCharsets.UTF_8), PublishCommentMqDTO.class))
                        .toList();

                // 批量查回复评论
                List<Long> replyCommentIds = dtos.stream()
                        .map(PublishCommentMqDTO::getReplyCommentId)
                        .filter(Objects::nonNull)
                        .filter(id -> id > 0)
                        .toList();
                Map<Long, CommentDO> replyCommentMap = Map.of();
                if (CollUtil.isNotEmpty(replyCommentIds)) {
                    List<CommentDO> replyDOs = commentDOMapper.selectByCommentIds(replyCommentIds);
                    if (CollUtil.isNotEmpty(replyDOs)) {
                        replyCommentMap = replyDOs.stream().collect(Collectors.toMap(CommentDO::getId, d -> d));
                    }
                }

                // 拼装 BO
                List<CommentBO> commentBOS = commentAssembler.assemble(dtos, replyCommentMap);
                log.info("## 清洗后的 CommentBOS: {}", JsonUtils.toJsonString(commentBOS));

                // 编程式事务：批量写 PG + RPC 写 Scylla
                transactionTemplate.execute(status -> {
                    try {
                        commentDOMapper.batchInsert(commentBOS);

                        List<CommentBO> contentNotEmpty = commentBOS.stream()
                                .filter(bo -> Boolean.FALSE.equals(bo.getIsContentEmpty()))
                                .toList();
                        if (CollUtil.isNotEmpty(contentNotEmpty)) {
                            keyValueRpcService.batchSaveCommentContent(contentNotEmpty);
                        }
                        return true;
                    } catch (Exception ex) {
                        status.setRollbackOnly();
                        log.error("==> 评论批量写库事务回滚: ", ex);
                        throw ex;
                    }
                });

                return ConsumeConcurrentlyStatus.CONSUME_SUCCESS;
            } catch (Exception e) {
                log.error("==> 评论批量写库失败，稍后重试: ", e);
                return ConsumeConcurrentlyStatus.RECONSUME_LATER;
            }
        });

        consumer.start();
        return consumer;
    }

    @PreDestroy
    public void destroy() {
        if (Objects.nonNull(consumer)) {
            try {
                consumer.shutdown();
            } catch (Exception e) {
                log.error("==> 评论消费者关闭异常: ", e);
            }
        }
    }
}
