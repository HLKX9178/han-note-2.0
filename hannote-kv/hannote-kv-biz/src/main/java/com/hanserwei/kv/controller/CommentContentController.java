package com.hanserwei.kv.controller;

import com.hanserwei.framework.common.response.Response;
import com.hanserwei.kv.api.dto.req.BatchAddCommentContentReqDTO;
import com.hanserwei.kv.service.CommentContentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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
}
