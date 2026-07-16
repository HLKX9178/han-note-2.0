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

    /**
     * 删除评论及其完整子树（事务内物理删除，提交后异步清理缓存/KV 内容/计数）.
     *
     * @param request 删除请求（评论 ID）
     * @return 统一成功响应
     * @throws com.hanserwei.framework.common.exception.BizException 评论不存在或非本人操作时抛出
     */
    Response<?> delete(DeleteCommentReqVO request);
}
