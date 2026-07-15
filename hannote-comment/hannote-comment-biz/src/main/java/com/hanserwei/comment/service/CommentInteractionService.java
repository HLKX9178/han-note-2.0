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

    Response<?> like(LikeCommentReqVO request);

    Response<?> unlike(LikeCommentReqVO request);
}
