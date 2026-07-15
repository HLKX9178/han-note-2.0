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
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.client.consumer.DefaultMQPushConsumer;
import org.apache.rocketmq.client.consumer.listener.ConsumeConcurrentlyStatus;
import org.apache.rocketmq.client.consumer.listener.MessageListenerConcurrently;
import org.apache.rocketmq.client.exception.MQClientException;
import org.apache.rocketmq.common.consumer.ConsumeFromWhere;
import org.apache.rocketmq.remoting.protocol.heartbeat.MessageModel;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 评论批量写库消费者.
 *
 * <p>用 rocketmq-client 原生 {@link DefaultMQPushConsumer} 批量消费 {@code PublishCommentTopic}，
 * 令牌桶限流后拼装 {@link CommentBO}，先 RPC 批量写 ScyllaDB 评论正文，再批量写 {@code t_comment}
 * （{@code ON CONFLICT DO NOTHING} 幂等）。两侧均幂等，失败 {@code RECONSUME_LATER} 重试至最终一致。
 *
 * <p>生命周期与 note/relation 的原生批量消费者一致，用 {@code @PostConstruct} 启动、
 * {@code @PreDestroy} 关闭；不用 {@code @Bean} 工厂方法，避免消费者 Bean 在容器刷新早期被实例化
 * 而干扰 RocketMQ starter 的自动装配。
 *
 * @author hanserwei
 * @date 2026/07/14
 * @since 0.0.1
 */
@Component
@Slf4j
public class Comment2DBConsumer {

    private final String namesrvAddr;
    private final CommentDOMapper commentDOMapper;
    private final CommentAssembler commentAssembler;
    private final KeyValueRpcService keyValueRpcService;

    private DefaultMQPushConsumer consumer;

    /** 令牌桶：每秒 1000（按数据库承受力调整） */
    private final RateLimiter rateLimiter = RateLimiter.create(1000);

    public Comment2DBConsumer(@Value("${rocketmq.name-server}") String namesrvAddr,
                              CommentDOMapper commentDOMapper,
                              CommentAssembler commentAssembler,
                              KeyValueRpcService keyValueRpcService) {
        this.namesrvAddr = namesrvAddr;
        this.commentDOMapper = commentDOMapper;
        this.commentAssembler = commentAssembler;
        this.keyValueRpcService = keyValueRpcService;
    }

    @PostConstruct
    public void init() throws MQClientException {
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
                log.debug("## 清洗后的 CommentBOS 条数: {}", commentBOS.size());

                // 先 RPC 写 ScyllaDB 正文，再写 PG 元数据（与 hannote-note 发布笔记同序）。
                // 理由：ScyllaDB 不参与 PG 事务，二者无法原子。若先写 PG 后写 Scylla，正文写失败会留下
                // 「有评论无正文」的用户可见损坏；反之 Scylla 中的孤儿正文以 contentUuid 为键、无人引用，
                // 不可见且无害。两侧均幂等（Scylla insert 覆盖 / PG ON CONFLICT DO NOTHING），
                // RECONSUME_LATER 重试即可最终一致。
                List<CommentBO> contentNotEmpty = commentBOS.stream()
                        .filter(bo -> Boolean.FALSE.equals(bo.getIsContentEmpty()))
                        .toList();
                if (CollUtil.isNotEmpty(contentNotEmpty)) {
                    keyValueRpcService.batchSaveCommentContent(contentNotEmpty);
                }

                // PG 批量写。RPC 已移出事务，避免 ScyllaDB 网络 IO 长时间占用事务连接。
                commentDOMapper.batchInsert(commentBOS);

                return ConsumeConcurrentlyStatus.CONSUME_SUCCESS;
            } catch (Exception e) {
                log.error("==> 评论批量写库失败，稍后重试: ", e);
                return ConsumeConcurrentlyStatus.RECONSUME_LATER;
            }
        });

        consumer.start();
        log.info("## Comment2DBConsumer 启动完成，批量消费 {}", MQConstants.TOPIC_PUBLISH_COMMENT);
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
