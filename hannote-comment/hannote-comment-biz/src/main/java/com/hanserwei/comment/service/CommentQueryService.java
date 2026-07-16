package com.hanserwei.comment.service;

import com.hanserwei.comment.model.response.CommentPageResponse;
import com.hanserwei.comment.model.vo.FindChildCommentItemRspVO;
import com.hanserwei.comment.model.vo.FindChildCommentPageListReqVO;
import com.hanserwei.comment.model.vo.FindCommentItemRspVO;
import com.hanserwei.comment.model.vo.FindCommentPageListReqVO;
import com.hanserwei.framework.common.response.PageResponse;

/**
 * 评论分页查询业务.
 *
 * @author hanserwei
 * @date 2026/07/15
 * @since 0.0.1
 */
public interface CommentQueryService {

    /**
     * 分页查询笔记的一级（根）评论，每条附带首条子回复.
     *
     * @param request 分页请求（笔记 ID + 页码）
     * @return 根评论分页结果，额外携带评论总数（含子评论）
     */
    CommentPageResponse<FindCommentItemRspVO> findRootComments(FindCommentPageListReqVO request);

    /**
     * 分页查询某条一级评论下的子评论（跳过已随根评论展示的首条回复）.
     *
     * @param request 分页请求（父评论 ID + 页码）
     * @return 子评论分页结果
     * @throws com.hanserwei.framework.common.exception.BizException 父评论不存在或非一级评论时抛出
     */
    PageResponse<FindChildCommentItemRspVO> findChildComments(FindChildCommentPageListReqVO request);
}
