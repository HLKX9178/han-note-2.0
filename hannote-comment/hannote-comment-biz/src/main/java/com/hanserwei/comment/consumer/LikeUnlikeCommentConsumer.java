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
 * <p>顺序消费 {@code LikeUnlikeCommentTopic}（同一评论的点赞/取消进同一队列，保证时序），单批先按
 * (userId, commentId) 合并为「最后一次操作」以抵消同一用户在批内的反复点赞/取消，再在同一事务内批量
 * 写点赞明细、批量删除取消明细并按 delta 更新评论点赞总数，最后刷新点赞数 Hash 并失效受影响的根评论
 * 热点列表。批量落库幂等，异常整批 {@code SUSPEND_CURRENT_QUEUE_A_MOMENT} 重试。
 *
 * <p>生命周期同 {@link Comment2DBConsumer}：{@code @PostConstruct} 启动、{@code @PreDestroy} 关闭。
 *
 * @author hanserwei
 * @date 2026/07/15
 * @since 0.0.1
 */
@Slf4j
@Component
public class LikeUnlikeCommentConsumer {

    /** RocketMQ NameServer 地址（从配置注入） */
    private final String namesrvAddr;
    /** 评论点赞明细 Mapper（批量写入/删除并返回真实变更行） */
    private final CommentLikeDOMapper commentLikeDOMapper;
    /** 评论元数据 Mapper（更新点赞总数、回查互动统计） */
    private final CommentDOMapper commentDOMapper;
    /** 编程式事务：明细增删与点赞总数更新在同一事务内 */
    private final TransactionTemplate transactionTemplate;
    /** 评论缓存管理器：点赞变更后刷新计数 Hash 并失效热点列表 */
    private final CommentCacheManager commentCacheManager;
    /** 消费参数（限流速率、批大小、最大重试次数） */
    private final CommentMqConsumerProperties properties;
    /** 令牌桶限流器（速率取自 {@link CommentMqConsumerProperties#getRateLimit()}） */
    private final RateLimiter rateLimiter;

    /** 原生顺序消费者实例，由 {@link #init()} 创建、{@link #destroy()} 关闭 */
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

    /**
     * 初始化并启动原生顺序消费者：订阅 {@code LikeUnlikeCommentTopic}（全 Tag），集群模式、
     * 顺序消费，批大小/最大重试次数取自配置.
     *
     * @throws MQClientException 消费者订阅/启动失败
     */
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
                // 令牌桶限流
                rateLimiter.acquire(messages.size());

                // 消息体 -> DTO 集合
                List<LikeUnlikeCommentMqDTO> source = messages.stream()
                        .map(message -> JsonUtils.parseObject(
                                new String(message.getBody(), StandardCharsets.UTF_8),
                                LikeUnlikeCommentMqDTO.class))
                        .filter(Objects::nonNull)
                        .toList();

                // 批内按 (userId, commentId) 合并为最后一次操作，抵消反复点赞/取消，再拆分点赞与取消
                List<LikeUnlikeCommentMqDTO> merged = InteractionMergeSupport.mergeByLastOperation(source);
                List<LikeUnlikeCommentMqDTO> likes = merged.stream()
                        .filter(item -> Objects.equals(item.getType(), CommentLikeUnlikeTypeEnum.LIKE.getCode()))
                        .toList();
                List<LikeUnlikeCommentMqDTO> unlikes = merged.stream()
                        .filter(item -> Objects.equals(item.getType(), CommentLikeUnlikeTypeEnum.UNLIKE.getCode()))
                        .toList();

                // 同一事务内批量增删明细并更新点赞总数，返回各评论点赞数增量
                Map<Long, Integer> deltas = transactionTemplate.execute(status -> applyChanges(likes, unlikes));

                // 有实际变更：回查互动统计，刷新计数 Hash 并失效受影响的根评论热点列表
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

    /**
     * 在事务内应用点赞/取消：批量写点赞明细、批量删取消明细，按真实变更行累加各评论点赞数增量并更新总数.
     *
     * <p>以「明细表返回的真实新增/删除行」而非消息条数计算 delta，实现幂等（重复点赞不重复计数）；
     * 净增量为 0 的评论会被剔除，不产生无谓的总数更新。
     *
     * @param likes   合并后的点赞消息
     * @param unlikes 合并后的取消点赞消息
     * @return 评论 ID -> 点赞数净增量（已剔除净增量为 0 的项）
     */
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

    /**
     * 容器销毁时关闭消费者，释放长连接与线程资源.
     */
    @PreDestroy
    public void destroy() {
        if (consumer != null) {
            consumer.shutdown();
        }
    }
}
