package com.hanserwei.comment.service;

import com.hanserwei.comment.model.vo.LikeCommentReqVO;
import com.hanserwei.framework.common.response.Response;

/**
 * 评论互动业务.
 *
 * @author hanserwei
 * @date 2026/07/15
 * @since 0.0.1
 */
public interface CommentInteractionService {

    /**
     * 对评论点赞（布隆过滤器快速判重 + DB 兜底，计数经 MQ 异步落库）.
     *
     * @param request 点赞请求（评论 ID）
     * @return 统一成功响应
     * @throws com.hanserwei.framework.common.exception.BizException 评论不存在或已点赞时抛出
     */
    Response<?> like(LikeCommentReqVO request);

    /**
     * 取消评论点赞（布隆过滤器判存 + DB 兜底，计数经 MQ 异步落库）.
     *
     * @param request 取消点赞请求（评论 ID）
     * @return 统一成功响应
     * @throws com.hanserwei.framework.common.exception.BizException 评论不存在或未点赞时抛出
     */
    Response<?> unlike(LikeCommentReqVO request);
}
