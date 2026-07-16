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

    /**
     * 批量查询评论正文.
     *
     * <p>ScyllaDB 以 (note_id, year_month) 为分区键，故先按发布年月分组，同一分区内用
     * {@code content_id IN (...)} 一次查回，避免逐条访问导致的跨分区扫描。命中不到的主键
     * 自然缺席，不视为异常。
     *
     * @param request 批量查询入参（同一笔记下的多个「年月 + 正文 ID」）
     * @return 评论正文集合；返回顺序不作承诺
     */
    @Override
    public Response<List<FindCommentContentRspDTO>> batchFindCommentContent(BatchFindCommentContentReqDTO request) {
        // 1. 按发布年月（分区键的一部分）分组，值为该月下待查询的正文 UUID 列表
        Map<String, List<UUID>> idsByMonth = request.getItems().stream()
                .collect(Collectors.groupingBy(
                        item -> item.getYearMonth(),
                        Collectors.mapping(item -> UUID.fromString(item.getContentId()), Collectors.toList())));
        // 2. 逐分区执行 IN 查询并汇总（同分区一次查回，规避跨分区扫描）
        List<CommentContentDO> contentDOS = idsByMonth.entrySet().stream()
                .flatMap(entry -> cassandraTemplate.select(Query.query(
                                Criteria.where("note_id").is(request.getNoteId()),
                                Criteria.where("year_month").is(entry.getKey()),
                                Criteria.where("content_id").in(entry.getValue())), CommentContentDO.class)
                        .stream())
                .toList();
        // 3. DO -> RspDTO
        List<FindCommentContentRspDTO> result = contentDOS.stream()
                .map(contentDO -> FindCommentContentRspDTO.builder()
                        .contentId(contentDO.getPrimaryKey().getContentId().toString())
                        .content(contentDO.getContent())
                        .build())
                .toList();
        return Response.success(result);
    }

    /**
     * 批量删除评论正文（幂等）.
     *
     * <p>同样按发布年月分区批量删除；主键不存在时 delete 静默通过，天然幂等，
     * 可安全用于评论删除链路的 MQ 消费失败重投兜底补偿。
     *
     * @param request 批量删除入参（同一笔记下的多个「年月 + 正文 ID」）
     * @return 操作结果
     */
    @Override
    public Response<?> batchDeleteCommentContent(BatchDeleteCommentContentReqDTO request) {
        // 1. 按发布年月（分区键的一部分）分组
        Map<String, List<com.hanserwei.kv.api.dto.req.CommentContentKeyReqDTO>> byMonth = request.getItems().stream()
                .collect(Collectors.groupingBy(item -> item.getYearMonth()));
        // 2. 同分区内组装仅含主键的 DO 并批量删除
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
