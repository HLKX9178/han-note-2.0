package com.hanserwei.comment.service.impl;

import com.google.common.base.Preconditions;
import com.hanserwei.comment.constant.MQConstants;
import com.hanserwei.comment.model.dto.PublishCommentMqDTO;
import com.hanserwei.comment.model.vo.PublishCommentReqVO;
import com.hanserwei.comment.retry.SendMqRetryHelper;
import com.hanserwei.comment.rpc.DistributedIdRpcService;
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
    private final SendMqRetryHelper sendMqRetryHelper;

    @Override
    public Response<?> publishComment(PublishCommentReqVO publishCommentReqVO) {
        String content = publishCommentReqVO.getContent();
        String imageUrl = publishCommentReqVO.getImageUrl();

        // 评论内容与图片不能同时为空
        Preconditions.checkArgument(StringUtils.isNotBlank(content) || StringUtils.isNotBlank(imageUrl),
                "评论正文和图片不能同时为空");

        // 发布者 ID（网关透传 userId）
        Long creatorId = LoginUserContextHolder.getUserId();

        // 预生成评论 ID（幂等基准）
        Long commentId = distributedIdRpcService.generateCommentId();
        if (Objects.isNull(commentId)) {
            throw new BizException(SYSTEM_ERROR);
        }

        // 构建消息体
        PublishCommentMqDTO publishCommentMqDTO = PublishCommentMqDTO.builder()
                .commentId(commentId)
                .noteId(publishCommentReqVO.getNoteId())
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
