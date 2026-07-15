package com.hanserwei.kv.service;

import com.hanserwei.framework.common.response.Response;
import com.hanserwei.kv.api.dto.req.BatchAddCommentContentReqDTO;
import com.hanserwei.kv.api.dto.req.BatchDeleteCommentContentReqDTO;
import com.hanserwei.kv.api.dto.req.BatchFindCommentContentReqDTO;
import com.hanserwei.kv.api.dto.resp.FindCommentContentRspDTO;

import java.util.List;

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

    /**
     * 批量查询评论正文.
     *
     * @param request 查询入参
     * @return 正文集合
     */
    Response<List<FindCommentContentRspDTO>> batchFindCommentContent(BatchFindCommentContentReqDTO request);

    /**
     * 批量删除评论正文.
     *
     * @param request 删除入参
     * @return 操作结果
     */
    Response<?> batchDeleteCommentContent(BatchDeleteCommentContentReqDTO request);
}
