package com.hanserwei.comment.rpc;

import com.hanserwei.comment.model.bo.CommentBO;
import com.hanserwei.framework.common.constant.DateConstants;
import com.hanserwei.framework.common.response.Response;
import com.hanserwei.kv.api.KeyValueHttpApi;
import com.hanserwei.kv.api.dto.req.BatchAddCommentContentReqDTO;
import com.hanserwei.kv.api.dto.req.CommentContentReqDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Objects;

/**
 * KV 键值服务调用封装（批量存评论正文到 ScyllaDB）.
 *
 * @author hanserwei
 * @date 2026/07/14
 * @since 0.0.1
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class KeyValueRpcService {

    private static final DateTimeFormatter YEAR_MONTH = DateTimeFormatter.ofPattern(DateConstants.Y_M);

    private final KeyValueHttpApi keyValueHttpApi;

    /**
     * 批量存储评论内容.
     *
     * @param commentBOS 内容非空的评论 BO 集合
     * @return 成功返回 {@code true}；失败抛异常以便调用方回滚事务
     */
    public boolean batchSaveCommentContent(List<CommentBO> commentBOS) {
        List<CommentContentReqDTO> comments = commentBOS.stream()
                .map(bo -> CommentContentReqDTO.builder()
                        .noteId(bo.getNoteId())
                        .content(bo.getContent())
                        .contentId(bo.getContentUuid())
                        .yearMonth(bo.getCreateTime().format(YEAR_MONTH))
                        .build())
                .toList();

        Response<?> response = keyValueHttpApi.batchAddCommentContent(
                BatchAddCommentContentReqDTO.builder().comments(comments).build());

        if (Objects.isNull(response) || !response.isSuccess()) {
            log.error("==> 批量保存评论内容失败, response: {}", response);
            throw new RuntimeException("批量保存评论内容失败");
        }
        return true;
    }
}
