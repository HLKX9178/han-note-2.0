package com.hanserwei.comment.service.impl;

import com.hanserwei.comment.cache.CommentCacheManager;
import com.hanserwei.comment.constant.MQConstants;
import com.hanserwei.comment.domain.dataobject.CommentDO;
import com.hanserwei.comment.domain.mapper.CommentDOMapper;
import com.hanserwei.comment.domain.mapper.CommentLikeDOMapper;
import com.hanserwei.comment.enums.CommentLevelEnum;
import com.hanserwei.comment.enums.ResponseCodeEnum;
import com.hanserwei.comment.model.dto.CommentCountChangedMqDTO;
import com.hanserwei.comment.model.dto.DeleteCommentContentItemMqDTO;
import com.hanserwei.comment.model.dto.DeleteCommentContentMqDTO;
import com.hanserwei.comment.model.dto.DeleteCommentLocalCacheMqDTO;
import com.hanserwei.comment.model.vo.DeleteCommentReqVO;
import com.hanserwei.comment.retry.SendMqRetryHelper;
import com.hanserwei.comment.service.CommentDeleteService;
import com.hanserwei.framework.biz.context.holder.LoginUserContextHolder;
import com.hanserwei.framework.common.constant.DateConstants;
import com.hanserwei.framework.common.exception.BizException;
import com.hanserwei.framework.common.response.Response;
import com.hanserwei.framework.common.util.JsonUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * 评论删除业务实现：PG 完整子树事务删除，提交后清理缓存/KV/计数.
 *
 * @author hanserwei
 * @date 2026/07/15
 * @since 0.0.1
 */
@Service
@RequiredArgsConstructor
public class CommentDeleteServiceImpl implements CommentDeleteService {

    /** MQ 分片大小：删除子树可能涉及大量评论，按 20 条一批拆分消息避免消息体过大 */
    private static final int MESSAGE_CHUNK_SIZE = 20;
    /** 年月格式化器，用于定位 KV 内容按年月分表的存储位置 */
    private static final DateTimeFormatter YEAR_MONTH = DateTimeFormatter.ofPattern(DateConstants.Y_M);

    private final CommentDOMapper commentDOMapper;
    private final CommentLikeDOMapper commentLikeDOMapper;
    /** 编程式事务模板：保证评论子树删除与点赞记录删除的原子性 */
    private final TransactionTemplate transactionTemplate;
    /** 评论本地缓存管理：删除后失效本地/Redis 缓存 */
    private final CommentCacheManager commentCacheManager;
    /** MQ 可靠发送助手：异步广播缓存失效、清理 KV 内容、修正计数 */
    private final SendMqRetryHelper sendMqRetryHelper;

    /**
     * 删除评论及其完整子树.
     *
     * <p>核心链路：先在编程式事务内校验权限并物理删除子树（评论 + 点赞记录），
     * 事务提交后再做副作用清理，避免事务未提交就清缓存造成读到脏数据。提交后依次：
     * 失效本地/Redis 缓存 → 广播其他实例失效本地缓存 → 分片发 MQ 清理 KV 正文内容
     * → 发 MQ 按删除条数修正笔记评论总数。
     *
     * @param request 删除请求（评论 ID）
     * @return 统一成功响应
     * @throws BizException 评论不存在或非本人操作时抛出
     */
    @Override
    public Response<?> delete(DeleteCommentReqVO request) {
        Long currentUserId = LoginUserContextHolder.getUserId();
        // 1. 事务内完成子树物理删除
        DeleteResult result = transactionTemplate.execute(status -> deleteInTransaction(
                request.getCommentId(), currentUserId));
        if (result == null || result.snapshots().isEmpty()) {
            throw new BizException(ResponseCodeEnum.COMMENT_NOT_FOUND);
        }

        // 2. 提交后清理本实例缓存并重建受影响的 ZSET
        commentCacheManager.afterCommentsDeleted(result.snapshots(), result.rootCommentId());
        // 3. 广播其他实例失效本地缓存
        sendLocalCacheBroadcast(result.commentIds());
        // 4. 分片发 MQ 清理 KV 正文内容
        sendDeleteContentMessages(result);
        // 5. 发 MQ 按删除条数修正笔记评论总数（delta 为负）
        sendMqRetryHelper.asyncSend(MQConstants.TOPIC_COMMENT_COUNT_CHANGED,
                JsonUtils.toJsonString(CommentCountChangedMqDTO.builder()
                        .eventId(UUID.randomUUID().toString())
                        .noteId(result.noteId())
                        .delta(-result.snapshots().size())
                        .build()));
        return Response.success();
    }

    /**
     * 事务内删除评论子树.
     *
     * <p>行锁（{@code selectByIdForUpdate}）锁定目标评论后校验归属；一级评论删除整棵子树，
     * 二级评论仅删除其回复分支。物理删除点赞记录与评论后，若删的是二级评论还需重算根评论统计
     * （回复数/首条回复等）。
     *
     * @param commentId     目标评论 ID
     * @param currentUserId 当前登录用户 ID
     * @return 删除结果快照（含笔记 ID、根评论 ID、被删评论 DO 与 ID 列表）
     * @throws BizException 评论不存在或非本人操作时抛出
     */
    private DeleteResult deleteInTransaction(Long commentId, Long currentUserId) {
        // 行锁锁定目标评论，防并发删除/统计错乱
        CommentDO target = commentDOMapper.selectByIdForUpdate(commentId);
        if (target == null) {
            throw new BizException(ResponseCodeEnum.COMMENT_NOT_FOUND);
        }
        if (!Objects.equals(target.getUserId(), currentUserId)) {
            throw new BizException(ResponseCodeEnum.COMMENT_OPERATION_FORBIDDEN);
        }

        // 一级评论删整棵子树，二级评论仅删其回复分支
        boolean rootComment = Objects.equals(target.getLevel(), CommentLevelEnum.ONE.getCode());
        List<CommentDO> snapshots = rootComment
                ? commentDOMapper.selectRootDeleteTargets(commentId)
                : commentDOMapper.selectReplyBranchDeleteTargets(commentId);
        List<Long> ids = snapshots.stream().map(CommentDO::getId).toList();
        commentLikeDOMapper.deleteByCommentIds(ids);
        commentDOMapper.deleteByIdsPhysically(ids);

        // 删二级评论：重算根评论统计（回复数/首条回复）
        Long rootCommentId = rootComment ? target.getId() : target.getParentId();
        if (!rootComment) {
            commentDOMapper.recomputeRootStatistics(List.of(rootCommentId));
        }
        return new DeleteResult(target.getNoteId(), rootCommentId, rootComment, snapshots, ids);
    }

    /**
     * 分片广播「失效本地缓存」消息，通知各实例清理被删评论的本地缓存.
     *
     * @param ids 被删评论 ID 列表
     */
    private void sendLocalCacheBroadcast(List<Long> ids) {
        for (List<Long> chunk : chunks(ids)) {
            sendMqRetryHelper.asyncSend(MQConstants.TOPIC_DELETE_COMMENT_LOCAL_CACHE,
                    JsonUtils.toJsonString(DeleteCommentLocalCacheMqDTO.builder().commentIds(chunk).build()));
        }
    }

    /**
     * 分片发送「清理 KV 正文内容」消息.
     *
     * <p>仅针对内容非空且有 contentUuid 的评论；按创建年月定位 KV 分表位置，分片发送。
     *
     * @param result 删除结果快照
     */
    private void sendDeleteContentMessages(DeleteResult result) {
        List<DeleteCommentContentItemMqDTO> contentItems = result.snapshots().stream()
                .filter(item -> Boolean.FALSE.equals(item.getIsContentEmpty()))
                .filter(item -> item.getContentUuid() != null && !item.getContentUuid().isBlank())
                .map(item -> DeleteCommentContentItemMqDTO.builder()
                        .yearMonth(item.getCreateTime().format(YEAR_MONTH))
                        .contentId(item.getContentUuid())
                        .build())
                .toList();
        for (List<DeleteCommentContentItemMqDTO> chunk : chunks(contentItems)) {
            sendMqRetryHelper.asyncSend(MQConstants.TOPIC_DELETE_COMMENT_CONTENT,
                    JsonUtils.toJsonString(DeleteCommentContentMqDTO.builder()
                            .noteId(result.noteId())
                            .items(chunk)
                            .build()));
        }
    }

    /**
     * 将列表按 {@link #MESSAGE_CHUNK_SIZE} 切分为多个不可变子列表.
     *
     * @param source 源列表
     * @param <T>    元素类型
     * @return 分片后的子列表集合
     */
    private <T> List<List<T>> chunks(List<T> source) {
        List<List<T>> chunks = new ArrayList<>();
        for (int start = 0; start < source.size(); start += MESSAGE_CHUNK_SIZE) {
            chunks.add(List.copyOf(source.subList(start, Math.min(start + MESSAGE_CHUNK_SIZE, source.size()))));
        }
        return chunks;
    }

    /**
     * 事务内删除结果快照.
     *
     * @param noteId        所属笔记 ID
     * @param rootCommentId 根评论 ID（一级为自身，二级为其父）
     * @param rootComment   目标是否为一级评论
     * @param snapshots     被删评论 DO 列表（用于后续清缓存/清内容）
     * @param commentIds    被删评论 ID 列表
     */
    private record DeleteResult(Long noteId, Long rootCommentId, boolean rootComment,
                                List<CommentDO> snapshots, List<Long> commentIds) {
    }
}
