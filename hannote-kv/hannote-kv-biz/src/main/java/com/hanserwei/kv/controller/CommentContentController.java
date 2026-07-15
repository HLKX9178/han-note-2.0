package com.hanserwei.kv.controller;

import com.hanserwei.framework.common.response.Response;
import com.hanserwei.kv.api.dto.req.BatchAddCommentContentReqDTO;
import com.hanserwei.kv.api.dto.req.BatchDeleteCommentContentReqDTO;
import com.hanserwei.kv.api.dto.req.BatchFindCommentContentReqDTO;
import com.hanserwei.kv.api.dto.resp.FindCommentContentRspDTO;
import com.hanserwei.kv.service.CommentContentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 评论内容存储控制器.
 *
 * @author hanserwei
 * @date 2026/07/14
 * @since 0.0.1
 */
@RestController
@RequestMapping("/kv")
@Slf4j
@RequiredArgsConstructor
public class CommentContentController {

    private final CommentContentService commentContentService;

    /**
     * 批量新增评论内容.
     */
    @PostMapping("/comment/content/batchAdd")
    public Response<?> batchAddCommentContent(@Validated @RequestBody BatchAddCommentContentReqDTO batchAddCommentContentReqDTO) {
        return commentContentService.batchAddCommentContent(batchAddCommentContentReqDTO);
    }

    /**
     * 批量查询评论正文.
     */
    @PostMapping("/comment/content/batchFind")
    public Response<List<FindCommentContentRspDTO>> batchFindCommentContent(
            @Validated @RequestBody BatchFindCommentContentReqDTO request) {
        return commentContentService.batchFindCommentContent(request);
    }

    /**
     * 批量删除评论正文.
     */
    @PostMapping("/comment/content/batchDelete")
    public Response<?> batchDeleteCommentContent(
            @Validated @RequestBody BatchDeleteCommentContentReqDTO request) {
        return commentContentService.batchDeleteCommentContent(request);
    }
}
