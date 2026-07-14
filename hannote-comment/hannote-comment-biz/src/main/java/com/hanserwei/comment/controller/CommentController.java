package com.hanserwei.comment.controller;

import com.hanserwei.comment.model.vo.PublishCommentReqVO;
import com.hanserwei.comment.service.CommentService;
import com.hanserwei.framework.biz.operationlog.aspect.ApiOperationLog;
import com.hanserwei.framework.common.response.Response;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 评论控制器.
 *
 * @author hanserwei
 * @date 2026/07/14
 * @since 0.0.1
 */
@RestController
@RequestMapping("/comment")
@Slf4j
@RequiredArgsConstructor
public class CommentController {

    private final CommentService commentService;

    /**
     * 发布评论.
     */
    @PostMapping("/publish")
    @ApiOperationLog(description = "发布评论")
    public Response<?> publishComment(@Validated @RequestBody PublishCommentReqVO publishCommentReqVO) {
        return commentService.publishComment(publishCommentReqVO);
    }
}
