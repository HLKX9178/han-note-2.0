package com.hanserwei.kv.service;

import com.hanserwei.framework.common.response.Response;
import com.hanserwei.kv.api.dto.req.BatchAddCommentContentReqDTO;

/**
 * 评论内容存储业务.
 *
 * @author hanserwei
 * @date 2026/07/14
 * @since 0.0.1
 */
public interface CommentContentService {

    /**
     * 批量新增评论内容.
     *
     * @param batchAddCommentContentReqDTO 批量新增入参
     * @return 操作结果
     */
    Response<?> batchAddCommentContent(BatchAddCommentContentReqDTO batchAddCommentContentReqDTO);
}
