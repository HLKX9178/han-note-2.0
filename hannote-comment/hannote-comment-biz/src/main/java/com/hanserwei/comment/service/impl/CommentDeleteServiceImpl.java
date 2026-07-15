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

    private static final int MESSAGE_CHUNK_SIZE = 20;
    private static final DateTimeFormatter YEAR_MONTH = DateTimeFormatter.ofPattern(DateConstants.Y_M);

    private final CommentDOMapper commentDOMapper;
    private final CommentLikeDOMapper commentLikeDOMapper;
    private final TransactionTemplate transactionTemplate;
    private final CommentCacheManager commentCacheManager;
    private final SendMqRetryHelper sendMqRetryHelper;

    @Override
    public Response<?> delete(DeleteCommentReqVO request) {
        Long currentUserId = LoginUserContextHolder.getUserId();
        DeleteResult result = transactionTemplate.execute(status -> deleteInTransaction(
                request.getCommentId(), currentUserId));
        if (result == null || result.snapshots().isEmpty()) {
            throw new BizException(ResponseCodeEnum.COMMENT_NOT_FOUND);
        }

        commentCacheManager.afterCommentsDeleted(result.snapshots(), result.rootCommentId());
        sendLocalCacheBroadcast(result.commentIds());
        sendDeleteContentMessages(result);
        sendMqRetryHelper.asyncSend(MQConstants.TOPIC_COMMENT_COUNT_CHANGED,
                JsonUtils.toJsonString(CommentCountChangedMqDTO.builder()
                        .eventId(UUID.randomUUID().toString())
                        .noteId(result.noteId())
                        .delta(-result.snapshots().size())
                        .build()));
        return Response.success();
    }

    private DeleteResult deleteInTransaction(Long commentId, Long currentUserId) {
        CommentDO target = commentDOMapper.selectByIdForUpdate(commentId);
        if (target == null) {
            throw new BizException(ResponseCodeEnum.COMMENT_NOT_FOUND);
        }
        if (!Objects.equals(target.getUserId(), currentUserId)) {
            throw new BizException(ResponseCodeEnum.COMMENT_OPERATION_FORBIDDEN);
        }

        boolean rootComment = Objects.equals(target.getLevel(), CommentLevelEnum.ONE.getCode());
        List<CommentDO> snapshots = rootComment
                ? commentDOMapper.selectRootDeleteTargets(commentId)
                : commentDOMapper.selectReplyBranchDeleteTargets(commentId);
        List<Long> ids = snapshots.stream().map(CommentDO::getId).toList();
        commentLikeDOMapper.deleteByCommentIds(ids);
        commentDOMapper.deleteByIdsPhysically(ids);

        Long rootCommentId = rootComment ? target.getId() : target.getParentId();
        if (!rootComment) {
            commentDOMapper.recomputeRootStatistics(List.of(rootCommentId));
        }
        return new DeleteResult(target.getNoteId(), rootCommentId, rootComment, snapshots, ids);
    }

    private void sendLocalCacheBroadcast(List<Long> ids) {
        for (List<Long> chunk : chunks(ids)) {
            sendMqRetryHelper.asyncSend(MQConstants.TOPIC_DELETE_COMMENT_LOCAL_CACHE,
                    JsonUtils.toJsonString(DeleteCommentLocalCacheMqDTO.builder().commentIds(chunk).build()));
        }
    }

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

    private <T> List<List<T>> chunks(List<T> source) {
        List<List<T>> chunks = new ArrayList<>();
        for (int start = 0; start < source.size(); start += MESSAGE_CHUNK_SIZE) {
            chunks.add(List.copyOf(source.subList(start, Math.min(start + MESSAGE_CHUNK_SIZE, source.size()))));
        }
        return chunks;
    }

    private record DeleteResult(Long noteId, Long rootCommentId, boolean rootComment,
                                List<CommentDO> snapshots, List<Long> commentIds) {
    }
}
