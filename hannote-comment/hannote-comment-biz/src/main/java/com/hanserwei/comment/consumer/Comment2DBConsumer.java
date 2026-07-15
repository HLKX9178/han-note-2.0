package com.hanserwei.comment.consumer;

import cn.hutool.core.collection.CollUtil;
import com.hanserwei.comment.assembler.CommentAssembler;
import com.hanserwei.comment.constant.MQConstants;
import com.hanserwei.comment.domain.dataobject.CommentDO;
import com.hanserwei.comment.domain.mapper.CommentDOMapper;
import com.hanserwei.comment.model.bo.CommentBO;
import com.hanserwei.comment.model.dto.PublishCommentMqDTO;
import com.hanserwei.comment.model.dto.CommentCountChangedMqDTO;
import com.hanserwei.comment.rpc.KeyValueRpcService;
import com.hanserwei.comment.retry.SendMqRetryHelper;
import com.hanserwei.comment.enums.CommentLevelEnum;
import com.hanserwei.comment.cache.CommentCacheManager;
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
import org.springframework.transaction.support.TransactionTemplate;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.ArrayList;
import java.util.Collections;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

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
    private final TransactionTemplate transactionTemplate;
    private final SendMqRetryHelper sendMqRetryHelper;
    private final CommentCacheManager commentCacheManager;

    private DefaultMQPushConsumer consumer;

    /** 令牌桶：每秒 1000（按数据库承受力调整） */
    private final RateLimiter rateLimiter = RateLimiter.create(1000);

    public Comment2DBConsumer(@Value("${rocketmq.name-server}") String namesrvAddr,
                              CommentDOMapper commentDOMapper,
                              CommentAssembler commentAssembler,
                              KeyValueRpcService keyValueRpcService,
                              TransactionTemplate transactionTemplate,
                              SendMqRetryHelper sendMqRetryHelper,
                              CommentCacheManager commentCacheManager) {
        this.namesrvAddr = namesrvAddr;
        this.commentDOMapper = commentDOMapper;
        this.commentAssembler = commentAssembler;
        this.keyValueRpcService = keyValueRpcService;
        this.transactionTemplate = transactionTemplate;
        this.sendMqRetryHelper = sendMqRetryHelper;
        this.commentCacheManager = commentCacheManager;
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
                rateLimiter.acquire(msgs.size());

                // 消息体 -> DTO 集合
                List<PublishCommentMqDTO> dtos = msgs.stream()
                        .map(msg -> JsonUtils.parseObject(new String(msg.getBody(), StandardCharsets.UTF_8), PublishCommentMqDTO.class))
                        .toList();

                // 批量查回复评论
                List<Long> replyCommentIds = dtos.stream()
                        .map(PublishCommentMqDTO::getReplyCommentId)
                        .filter(Objects::nonNull)
                        .filter(id -> id > 0)
                        .distinct()
                        .toList();
                Map<Long, CommentDO> replyCommentMap = Map.of();
                if (CollUtil.isNotEmpty(replyCommentIds)) {
                    List<CommentDO> replyDOs = commentDOMapper.selectByCommentIds(replyCommentIds);
                    if (CollUtil.isNotEmpty(replyDOs)) {
                        replyCommentMap = new HashMap<>(replyDOs.stream()
                                .collect(Collectors.toMap(CommentDO::getId, d -> d)));
                    }
                }
                // 同一批次可能同时包含“父评论消息 + 回复消息”。把批内尚未落库的父评论递归解析为
                // 最小投影，避免整批因查库暂时不存在而永久重试、父评论也无法先落库。
                replyCommentMap = resolveBatchReplyComments(dtos, replyCommentMap);

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

                // PG 批量写并返回真实新增行。只有真实新增行才能触发回复统计与跨服务计数副作用。
                List<CommentDO> inserted = transactionTemplate.execute(status -> {
                    List<Long> repliedIds = commentBOS.stream()
                            .map(CommentBO::getReplyCommentId)
                            .filter(id -> id != null && id > 0)
                            .distinct()
                            .toList();
                    Set<Long> batchCommentIds = commentBOS.stream().map(CommentBO::getId).collect(Collectors.toSet());
                    List<Long> persistedReplyIds = repliedIds.stream()
                            .filter(id -> !batchCommentIds.contains(id))
                            .toList();
                    if (CollUtil.isNotEmpty(persistedReplyIds)
                            && commentDOMapper.lockReplyComments(persistedReplyIds).size() != persistedReplyIds.size()) {
                        throw new IllegalStateException("被回复评论已删除，拒绝写入孤儿回复");
                    }
                    List<CommentDO> actualInserted = commentDOMapper.batchInsertReturning(commentBOS);
                    if (CollUtil.isEmpty(actualInserted)) {
                        return Collections.emptyList();
                    }
                    List<Long> rootCommentIds = actualInserted.stream()
                            .filter(item -> Objects.equals(item.getLevel(), CommentLevelEnum.TWO.getCode()))
                            .map(CommentDO::getParentId)
                            .filter(Objects::nonNull)
                            .distinct()
                            .toList();
                    if (CollUtil.isNotEmpty(rootCommentIds)) {
                        commentDOMapper.recomputeRootStatistics(rootCommentIds);
                    }
                    return new ArrayList<>(actualInserted);
                });

                if (CollUtil.isNotEmpty(inserted)) {
                    commentCacheManager.afterCommentsInserted(inserted);
                    Map<Long, Long> countByNoteId = inserted.stream()
                            .collect(Collectors.groupingBy(CommentDO::getNoteId, Collectors.counting()));
                    countByNoteId.forEach((noteId, count) -> sendMqRetryHelper.asyncSend(
                            MQConstants.TOPIC_COMMENT_COUNT_CHANGED,
                            JsonUtils.toJsonString(CommentCountChangedMqDTO.builder()
                                    .eventId(UUID.randomUUID().toString())
                                    .noteId(noteId)
                                    .delta(Math.toIntExact(count))
                                    .build())));
                }

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

    private Map<Long, CommentDO> resolveBatchReplyComments(List<PublishCommentMqDTO> dtos,
                                                            Map<Long, CommentDO> persisted) {
        Map<Long, CommentDO> resolved = new HashMap<>(persisted);
        Map<Long, PublishCommentMqDTO> batchById = dtos.stream()
                .collect(Collectors.toMap(PublishCommentMqDTO::getCommentId, item -> item, (left, right) -> left));
        dtos.stream().map(PublishCommentMqDTO::getReplyCommentId)
                .filter(id -> id != null && id > 0)
                .forEach(id -> resolveBatchComment(id, batchById, resolved, new HashSet<>()));
        return resolved;
    }

    private CommentDO resolveBatchComment(Long commentId,
                                           Map<Long, PublishCommentMqDTO> batchById,
                                           Map<Long, CommentDO> resolved,
                                           Set<Long> visiting) {
        CommentDO existing = resolved.get(commentId);
        if (existing != null) {
            return existing;
        }
        PublishCommentMqDTO dto = batchById.get(commentId);
        if (dto == null) {
            return null;
        }
        if (!visiting.add(commentId)) {
            throw new IllegalStateException("批内评论回复关系存在环, commentId=" + commentId);
        }
        Long repliedId = dto.getReplyCommentId();
        CommentDO current;
        if (repliedId == null || repliedId <= 0) {
            current = CommentDO.builder()
                    .id(dto.getCommentId()).noteId(dto.getNoteId()).userId(dto.getCreatorId())
                    .level(CommentLevelEnum.ONE.getCode()).parentId(dto.getNoteId()).build();
        } else {
            CommentDO replied = resolveBatchComment(repliedId, batchById, resolved, visiting);
            if (replied == null) {
                return null;
            }
            current = CommentDO.builder()
                    .id(dto.getCommentId()).noteId(dto.getNoteId()).userId(dto.getCreatorId())
                    .level(CommentLevelEnum.TWO.getCode())
                    .parentId(Objects.equals(replied.getLevel(), CommentLevelEnum.TWO.getCode())
                            ? replied.getParentId() : replied.getId())
                    .build();
        }
        visiting.remove(commentId);
        resolved.put(commentId, current);
        return current;
    }
}
