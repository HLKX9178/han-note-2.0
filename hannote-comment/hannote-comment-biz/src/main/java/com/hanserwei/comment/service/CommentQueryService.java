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

    CommentPageResponse<FindCommentItemRspVO> findRootComments(FindCommentPageListReqVO request);

    PageResponse<FindChildCommentItemRspVO> findChildComments(FindChildCommentPageListReqVO request);
}
