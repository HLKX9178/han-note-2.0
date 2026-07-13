package com.hanserwei.search.service;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch.core.BulkResponse;
import co.elastic.clients.elasticsearch.core.bulk.BulkOperation;
import com.hanserwei.search.domain.mapper.SelectMapper;
import com.hanserwei.search.index.NoteIndex;
import com.hanserwei.search.index.UserIndex;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * ES 索引增量同步业务.
 *
 * <p>供 MQ 消费者调用：按 noteId / userId 从 PostgreSQL 重查文档全字段后写入 / 删除 ES。
 * 所有写操作幂等（按文档 id 全量覆盖或删除）。失败向上抛出，交由 ORDERLY 消费者重试。
 *
 * @author hanserwei
 * @date 2026/07/13
 * @since 0.0.1
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class EsSyncService {

    private final ElasticsearchClient elasticsearchClient;
    private final SelectMapper selectMapper;

    /**
     * 重建单篇笔记文档。若重查为空（笔记已非公开 / 非正常展示 / 不存在），保险起见删除该文档。
     */
    public void rebuildNote(Long noteId) {
        List<Map<String, Object>> rows = selectMapper.selectEsNoteIndexData(noteId, null);
        if (rows.isEmpty()) {
            log.info("==> 笔记不满足索引条件（非公开/非正常/不存在），改为删除文档, noteId: {}", noteId);
            deleteNote(noteId);
            return;
        }
        rows.forEach(row -> indexDoc(NoteIndex.NAME, row, NoteIndex.FIELD_NOTE_ID));
        log.info("==> 重建笔记 ES 文档完成, noteId: {}", noteId);
    }

    /**
     * 删除单篇笔记文档。
     */
    public void deleteNote(Long noteId) {
        try {
            elasticsearchClient.delete(d -> d.index(NoteIndex.NAME).id(String.valueOf(noteId)));
            log.info("==> 删除笔记 ES 文档完成, noteId: {}", noteId);
        } catch (IOException e) {
            throw new UncheckedIOException("删除笔记 ES 文档失败, noteId: " + noteId, e);
        }
    }

    /**
     * 仅重建用户文档。
     */
    public void rebuildUser(Long userId) {
        List<Map<String, Object>> rows = selectMapper.selectEsUserIndexData(userId);
        if (rows.isEmpty()) {
            log.info("==> 用户不满足索引条件（禁用/已删除/不存在），跳过, userId: {}", userId);
            return;
        }
        rows.forEach(row -> indexDoc(UserIndex.NAME, row, UserIndex.FIELD_USER_ID));
        log.info("==> 重建用户 ES 文档完成, userId: {}", userId);
    }

    /**
     * 重建用户文档 + 该用户全部笔记文档（昵称 / 头像变更时，笔记索引冗余的发布者信息需同步）。
     */
    public void rebuildUserAndNotes(Long userId) {
        rebuildUser(userId);

        List<Map<String, Object>> notes = selectMapper.selectEsNoteIndexData(null, userId);
        if (notes.isEmpty()) {
            return;
        }
        bulkIndex(NoteIndex.NAME, notes, NoteIndex.FIELD_NOTE_ID);
        log.info("==> 重建用户名下 {} 篇笔记 ES 文档完成, userId: {}", notes.size(), userId);
    }

    /**
     * 单条写入（覆盖）ES 文档，文档 id 取 map 中主键字段值。
     */
    private void indexDoc(String index, Map<String, Object> doc, String idField) {
        String id = String.valueOf(doc.get(idField));
        try {
            elasticsearchClient.index(i -> i.index(index).id(id).document(doc));
        } catch (IOException e) {
            throw new UncheckedIOException("写入 ES 文档失败, index: " + index + ", id: " + id, e);
        }
    }

    /**
     * 批量写入（覆盖）ES 文档。
     */
    private void bulkIndex(String index, List<Map<String, Object>> docs, String idField) {
        List<BulkOperation> operations = new ArrayList<>(docs.size());
        for (Map<String, Object> doc : docs) {
            String id = String.valueOf(doc.get(idField));
            operations.add(BulkOperation.of(op -> op.index(idx -> idx.index(index).id(id).document(doc))));
        }

        try {
            BulkResponse response = elasticsearchClient.bulk(b -> b.operations(operations));
            if (response.errors()) {
                response.items().stream()
                        .filter(item -> item.error() != null)
                        .limit(5)
                        .forEach(item -> log.error("==> bulk 写入错误, id: {}, error: {}",
                                item.id(), item.error().reason()));
                throw new IllegalStateException("bulk 写入 ES 存在错误, index: " + index);
            }
        } catch (IOException e) {
            throw new UncheckedIOException("bulk 写入 ES 失败, index: " + index, e);
        }
    }
}
