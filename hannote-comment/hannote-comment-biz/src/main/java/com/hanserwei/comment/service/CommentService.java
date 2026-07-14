package com.hanserwei.comment.service;

import com.hanserwei.comment.model.vo.PublishCommentReqVO;
import com.hanserwei.framework.common.response.Response;

/**
 * 评论业务.
 *
 * @author hanserwei
 * @date 2026/07/14
 * @since 0.0.1
 */
public interface CommentService {

    /**
     * 发布评论（异步：预生成 ID + 可靠发 MQ，落库在消费端）.
     *
     * @param publishCommentReqVO 发布入参
     * @return 操作结果
     */
    Response<?> publishComment(PublishCommentReqVO publishCommentReqVO);
}
