package com.hanserwei.kv.service.impl;

import com.hanserwei.framework.common.response.Response;
import com.hanserwei.kv.api.dto.req.BatchAddCommentContentReqDTO;
import com.hanserwei.kv.api.dto.req.CommentContentReqDTO;
import com.hanserwei.kv.domain.dataobject.CommentContentDO;
import com.hanserwei.kv.domain.dataobject.CommentContentPrimaryKey;
import com.hanserwei.kv.service.CommentContentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.cassandra.core.CassandraTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

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
}
