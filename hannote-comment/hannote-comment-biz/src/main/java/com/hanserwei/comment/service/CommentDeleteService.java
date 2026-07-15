package com.hanserwei.comment.service;

import com.hanserwei.comment.model.vo.DeleteCommentReqVO;
import com.hanserwei.framework.common.response.Response;

/**
 * 评论删除业务.
 *
 * @author hanserwei
 * @date 2026/07/15
 * @since 0.0.1
 */
public interface CommentDeleteService {
    Response<?> delete(DeleteCommentReqVO request);
}
