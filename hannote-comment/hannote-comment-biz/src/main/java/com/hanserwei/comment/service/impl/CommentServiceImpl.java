package com.hanserwei.comment.service.impl;

import com.google.common.base.Preconditions;
import com.hanserwei.comment.constant.MQConstants;
import com.hanserwei.comment.model.dto.PublishCommentMqDTO;
import com.hanserwei.comment.model.vo.PublishCommentReqVO;
import com.hanserwei.comment.retry.SendMqRetryHelper;
import com.hanserwei.comment.rpc.DistributedIdRpcService;
import com.hanserwei.comment.rpc.NoteRpcService;
import com.hanserwei.comment.service.CommentService;
import com.hanserwei.framework.biz.context.holder.LoginUserContextHolder;
import com.hanserwei.framework.common.exception.BizException;
import com.hanserwei.framework.common.response.Response;
import com.hanserwei.framework.common.util.JsonUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

import static com.hanserwei.comment.enums.ResponseCodeEnum.SYSTEM_ERROR;

/**
 * 评论业务实现.
 *
 * @author hanserwei
 * @date 2026/07/14
 * @since 0.0.1
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class CommentServiceImpl implements CommentService {

    private final DistributedIdRpcService distributedIdRpcService;
    private final NoteRpcService noteRpcService;
    private final SendMqRetryHelper sendMqRetryHelper;

    /**
     * 发布评论（同步校验 + 预生成 ID，落库经 MQ 异步完成）.
     *
     * <p>正文与图片不可同时为空；发 MQ 前 RPC 校验笔记已发布，避免为不存在/已删笔记制造脏数据；
     * 预生成分布式评论 ID 作为幂等基准，最终经可靠 MQ 异步落库。
     *
     * @param publishCommentReqVO 发布请求（笔记 ID / 正文 / 图片 / 被回复评论 ID）
     * @return 统一成功响应
     * @throws BizException 笔记不存在或分布式 ID 生成失败时抛出
     */
    @Override
    public Response<?> publishComment(PublishCommentReqVO publishCommentReqVO) {
        String content = publishCommentReqVO.getContent();
        String imageUrl = publishCommentReqVO.getImageUrl();

        // 评论内容与图片不能同时为空
        Preconditions.checkArgument(StringUtils.isNotBlank(content) || StringUtils.isNotBlank(imageUrl),
                "评论正文和图片不能同时为空");

        // 发布者 ID（网关透传 userId）
        Long creatorId = LoginUserContextHolder.getUserId();

        // 发 MQ 前校验笔记存在，避免为已删除/不存在笔记制造异步评论数据。
        noteRpcService.requirePublished(publishCommentReqVO.getNoteId());

        // 预生成评论 ID（幂等基准）
        Long commentId = distributedIdRpcService.generateCommentId();
        if (Objects.isNull(commentId)) {
            throw new BizException(SYSTEM_ERROR);
        }

        // 构建消息体
        PublishCommentMqDTO publishCommentMqDTO = PublishCommentMqDTO.builder()
                .commentId(commentId)
                .noteId(publishCommentReqVO.getNoteId())
                .contentUuid(StringUtils.isBlank(content) ? "" : UUID.randomUUID().toString())
                .content(content)
                .imageUrl(imageUrl)
                .replyCommentId(publishCommentReqVO.getReplyCommentId())
                .createTime(LocalDateTime.now())
                .creatorId(creatorId)
                .build();

        // 可靠异步发送 MQ
        sendMqRetryHelper.asyncSend(MQConstants.TOPIC_PUBLISH_COMMENT, JsonUtils.toJsonString(publishCommentMqDTO));

        return Response.success();
    }
}
