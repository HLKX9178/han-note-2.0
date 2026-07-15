package com.hanserwei.kv.service.impl;

import com.hanserwei.framework.common.response.Response;
import com.hanserwei.kv.api.dto.req.BatchAddCommentContentReqDTO;
import com.hanserwei.kv.api.dto.req.BatchDeleteCommentContentReqDTO;
import com.hanserwei.kv.api.dto.req.BatchFindCommentContentReqDTO;
import com.hanserwei.kv.api.dto.req.CommentContentReqDTO;
import com.hanserwei.kv.api.dto.resp.FindCommentContentRspDTO;
import com.hanserwei.kv.domain.dataobject.CommentContentDO;
import com.hanserwei.kv.domain.dataobject.CommentContentPrimaryKey;
import com.hanserwei.kv.service.CommentContentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.cassandra.core.CassandraTemplate;
import org.springframework.data.cassandra.core.query.Criteria;
import org.springframework.data.cassandra.core.query.Query;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 评论内容存储业务实现（底层对接 ScyllaDB，单批写入保证幂等）.
 *
 * @author hanserwei
 * @date 2026/07/14
 * @since 0.0.1
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class CommentContentServiceImpl implements CommentContentService {

    private final CassandraTemplate cassandraTemplate;

    @Override
    public Response<?> batchAddCommentContent(BatchAddCommentContentReqDTO batchAddCommentContentReqDTO) {
        List<CommentContentReqDTO> comments = batchAddCommentContentReqDTO.getComments();

        // DTO -> DO
        List<CommentContentDO> contentDOS = comments.stream()
                .map(dto -> CommentContentDO.builder()
                        .primaryKey(CommentContentPrimaryKey.builder()
                                .noteId(dto.getNoteId())
                                .yearMonth(dto.getYearMonth())
                                .contentId(UUID.fromString(dto.getContentId()))
                                .build())
                        .content(dto.getContent())
                        .build())
                .toList();

        // 单批写入（同主键重复 insert 覆盖，天然幂等）
        cassandraTemplate.batchOps().insert(contentDOS).execute();
        log.info("==> 批量保存评论内容成功, size: {}", contentDOS.size());

        return Response.success();
    }

    @Override
    public Response<List<FindCommentContentRspDTO>> batchFindCommentContent(BatchFindCommentContentReqDTO request) {
        Map<String, List<UUID>> idsByMonth = request.getItems().stream()
                .collect(Collectors.groupingBy(
                        item -> item.getYearMonth(),
                        Collectors.mapping(item -> UUID.fromString(item.getContentId()), Collectors.toList())));
        List<CommentContentDO> contentDOS = idsByMonth.entrySet().stream()
                .flatMap(entry -> cassandraTemplate.select(Query.query(
                                Criteria.where("note_id").is(request.getNoteId()),
                                Criteria.where("year_month").is(entry.getKey()),
                                Criteria.where("content_id").in(entry.getValue())), CommentContentDO.class)
                        .stream())
                .toList();
        List<FindCommentContentRspDTO> result = contentDOS.stream()
                .map(contentDO -> FindCommentContentRspDTO.builder()
                        .contentId(contentDO.getPrimaryKey().getContentId().toString())
                        .content(contentDO.getContent())
                        .build())
                .toList();
        return Response.success(result);
    }

    @Override
    public Response<?> batchDeleteCommentContent(BatchDeleteCommentContentReqDTO request) {
        Map<String, List<com.hanserwei.kv.api.dto.req.CommentContentKeyReqDTO>> byMonth = request.getItems().stream()
                .collect(Collectors.groupingBy(item -> item.getYearMonth()));
        byMonth.forEach((yearMonth, items) -> {
            List<CommentContentDO> samePartition = items.stream()
                    .map(item -> CommentContentDO.builder()
                            .primaryKey(CommentContentPrimaryKey.builder()
                                    .noteId(request.getNoteId())
                                    .yearMonth(yearMonth)
                                    .contentId(UUID.fromString(item.getContentId()))
                                    .build())
                            .build())
                    .toList();
            cassandraTemplate.batchOps().delete(samePartition).execute();
        });
        log.info("==> 批量删除评论内容成功, noteId: {}, size: {}", request.getNoteId(), request.getItems().size());
        return Response.success();
    }
}
