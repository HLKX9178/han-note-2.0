package com.hanserwei.comment.consumer;

import com.google.common.util.concurrent.RateLimiter;
import com.hanserwei.comment.cache.CommentCacheManager;
import com.hanserwei.comment.config.CommentMqConsumerProperties;
import com.hanserwei.comment.constant.MQConstants;
import com.hanserwei.comment.domain.dataobject.CommentDO;
import com.hanserwei.comment.domain.dataobject.CommentLikeDO;
import com.hanserwei.comment.domain.mapper.CommentDOMapper;
import com.hanserwei.comment.domain.mapper.CommentLikeDOMapper;
import com.hanserwei.comment.enums.CommentLikeUnlikeTypeEnum;
import com.hanserwei.comment.model.dto.LikeUnlikeCommentMqDTO;
import com.hanserwei.comment.util.InteractionMergeSupport;
import com.hanserwei.framework.common.util.JsonUtils;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.client.consumer.DefaultMQPushConsumer;
import org.apache.rocketmq.client.consumer.listener.ConsumeOrderlyStatus;
import org.apache.rocketmq.client.consumer.listener.MessageListenerOrderly;
import org.apache.rocketmq.client.exception.MQClientException;
import org.apache.rocketmq.common.consumer.ConsumeFromWhere;
import org.apache.rocketmq.remoting.protocol.heartbeat.MessageModel;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 评论点赞/取消点赞顺序批量落库消费者.
 *
 * @author hanserwei
 * @date 2026/07/15
 * @since 0.0.1
 */
@Slf4j
@Component
public class LikeUnlikeCommentConsumer {

    private final String namesrvAddr;
    private final CommentLikeDOMapper commentLikeDOMapper;
    private final CommentDOMapper commentDOMapper;
    private final TransactionTemplate transactionTemplate;
    private final CommentCacheManager commentCacheManager;
    private final CommentMqConsumerProperties properties;
    private final RateLimiter rateLimiter;

    private DefaultMQPushConsumer consumer;

    public LikeUnlikeCommentConsumer(@Value("${rocketmq.name-server}") String namesrvAddr,
                                     CommentLikeDOMapper commentLikeDOMapper,
                                     CommentDOMapper commentDOMapper,
                                     TransactionTemplate transactionTemplate,
                                     CommentCacheManager commentCacheManager,
                                     CommentMqConsumerProperties properties) {
        this.namesrvAddr = namesrvAddr;
        this.commentLikeDOMapper = commentLikeDOMapper;
        this.commentDOMapper = commentDOMapper;
        this.transactionTemplate = transactionTemplate;
        this.commentCacheManager = commentCacheManager;
        this.properties = properties;
        this.rateLimiter = RateLimiter.create(properties.getRateLimit());
    }

    @PostConstruct
    public void init() throws MQClientException {
        consumer = new DefaultMQPushConsumer("hannote_comment_like_unlike_group");
        consumer.setNamesrvAddr(namesrvAddr);
        consumer.subscribe(MQConstants.TOPIC_LIKE_UNLIKE_COMMENT, "*");
        consumer.setConsumeFromWhere(ConsumeFromWhere.CONSUME_FROM_LAST_OFFSET);
        consumer.setMessageModel(MessageModel.CLUSTERING);
        consumer.setConsumeMessageBatchMaxSize(properties.getBatchSize());
        consumer.setMaxReconsumeTimes(properties.getMaxReconsumeTimes());
        consumer.registerMessageListener((MessageListenerOrderly) (messages, context) -> {
            try {
                rateLimiter.acquire(messages.size());
                List<LikeUnlikeCommentMqDTO> source = messages.stream()
                        .map(message -> JsonUtils.parseObject(
                                new String(message.getBody(), StandardCharsets.UTF_8),
                                LikeUnlikeCommentMqDTO.class))
                        .filter(Objects::nonNull)
                        .toList();
                List<LikeUnlikeCommentMqDTO> merged = InteractionMergeSupport.mergeByLastOperation(source);
                List<LikeUnlikeCommentMqDTO> likes = merged.stream()
                        .filter(item -> Objects.equals(item.getType(), CommentLikeUnlikeTypeEnum.LIKE.getCode()))
                        .toList();
                List<LikeUnlikeCommentMqDTO> unlikes = merged.stream()
                        .filter(item -> Objects.equals(item.getType(), CommentLikeUnlikeTypeEnum.UNLIKE.getCode()))
                        .toList();

                Map<Long, Integer> deltas = transactionTemplate.execute(status -> applyChanges(likes, unlikes));
                if (deltas != null && !deltas.isEmpty()) {
                    List<CommentDO> stats = commentDOMapper.selectInteractionStatsByIds(
                            List.copyOf(deltas.keySet()));
                    commentCacheManager.afterLikeChanged(deltas, stats);
                }
                return ConsumeOrderlyStatus.SUCCESS;
            } catch (Exception e) {
                log.error("==> 评论点赞批量落库失败，稍后重试, size: {}", messages.size(), e);
                return ConsumeOrderlyStatus.SUSPEND_CURRENT_QUEUE_A_MOMENT;
            }
        });
        consumer.start();
        log.info("## LikeUnlikeCommentConsumer 启动完成");
    }

    private Map<Long, Integer> applyChanges(List<LikeUnlikeCommentMqDTO> likes,
                                             List<LikeUnlikeCommentMqDTO> unlikes) {
        List<CommentLikeDO> inserted = likes.isEmpty()
                ? Collections.emptyList() : commentLikeDOMapper.batchInsertReturning(likes);
        List<CommentLikeDO> deleted = unlikes.isEmpty()
                ? Collections.emptyList() : commentLikeDOMapper.batchDeleteReturning(unlikes);

        Map<Long, Integer> deltas = new LinkedHashMap<>();
        inserted.forEach(item -> deltas.merge(item.getCommentId(), 1, Integer::sum));
        deleted.forEach(item -> deltas.merge(item.getCommentId(), -1, Integer::sum));
        deltas.entrySet().removeIf(entry -> entry.getValue() == 0);
        deltas.forEach(commentDOMapper::updateLikeTotal);
        return deltas;
    }

    @PreDestroy
    public void destroy() {
        if (consumer != null) {
            consumer.shutdown();
        }
    }
}
