package com.hanserwei.comment.rpc;

import com.hanserwei.comment.model.bo.CommentBO;
import com.hanserwei.framework.common.constant.DateConstants;
import com.hanserwei.framework.common.response.Response;
import com.hanserwei.kv.api.KeyValueHttpApi;
import com.hanserwei.kv.api.dto.req.BatchAddCommentContentReqDTO;
import com.hanserwei.kv.api.dto.req.CommentContentReqDTO;
import com.hanserwei.kv.api.dto.req.BatchFindCommentContentReqDTO;
import com.hanserwei.kv.api.dto.req.CommentContentKeyReqDTO;
import com.hanserwei.kv.api.dto.resp.FindCommentContentRspDTO;
import com.hanserwei.comment.domain.dataobject.CommentDO;
import com.hanserwei.kv.api.dto.req.BatchDeleteCommentContentReqDTO;
import com.hanserwei.comment.model.dto.DeleteCommentContentMqDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Objects;
import java.util.Map;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.stream.Collectors;

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

    /**
     * 按笔记分组批量查询评论正文.
     *
     * @param comments 评论元数据
     * @return contentUuid -> 正文
     */
    public Map<String, String> batchFindCommentContent(List<CommentDO> comments) {
        Map<String, String> result = new HashMap<>();
        Map<Long, List<CommentDO>> byNoteId = comments.stream()
                .filter(item -> Boolean.FALSE.equals(item.getIsContentEmpty()))
                .filter(item -> item.getContentUuid() != null && !item.getContentUuid().isBlank())
                .collect(Collectors.groupingBy(CommentDO::getNoteId));

        byNoteId.forEach((noteId, items) -> {
            List<CommentContentKeyReqDTO> keys = items.stream()
                    .map(item -> CommentContentKeyReqDTO.builder()
                            .yearMonth(item.getCreateTime().format(YEAR_MONTH))
                            .contentId(item.getContentUuid())
                            .build())
                    .toList();
            Response<List<FindCommentContentRspDTO>> response = keyValueHttpApi.batchFindCommentContent(
                    BatchFindCommentContentReqDTO.builder().noteId(noteId).items(keys).build());
            if (Objects.isNull(response) || !response.isSuccess() || Objects.isNull(response.getData())) {
                throw new IllegalStateException("批量查询评论内容失败, noteId=" + noteId);
            }
            response.getData().forEach(item -> result.put(item.getContentId(), item.getContent()));
            List<String> missing = keys.stream().map(CommentContentKeyReqDTO::getContentId)
                    .filter(contentId -> !result.containsKey(contentId))
                    .toList();
            if (!missing.isEmpty()) {
                throw new IllegalStateException("评论正文缺失, noteId=" + noteId + ", contentIds=" + missing);
            }
        });
        return result;
    }

    /**
     * 批量删除评论正文；失败抛异常触发 MQ 重试.
     */
    public void batchDeleteCommentContent(DeleteCommentContentMqDTO message) {
        List<CommentContentKeyReqDTO> items = message.getItems().stream()
                .map(item -> CommentContentKeyReqDTO.builder()
                        .yearMonth(item.getYearMonth())
                        .contentId(item.getContentId())
                        .build())
                .toList();
        Response<?> response = keyValueHttpApi.batchDeleteCommentContent(
                BatchDeleteCommentContentReqDTO.builder()
                        .noteId(message.getNoteId())
                        .items(items)
                        .build());
        if (response == null || !response.isSuccess()) {
            throw new IllegalStateException("批量删除评论正文失败, noteId=" + message.getNoteId());
        }
    }
}
