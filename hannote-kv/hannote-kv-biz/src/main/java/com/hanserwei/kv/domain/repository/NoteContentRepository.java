package com.hanserwei.kv.domain.repository;

import com.hanserwei.kv.domain.dataobject.NoteContentDO;
import org.springframework.data.cassandra.repository.CassandraRepository;

import java.util.UUID;

/**
 * 笔记内容 Repository.
 *
 * <p>继承 {@link CassandraRepository}，由 Spring Data Cassandra 提供针对
 * {@link NoteContentDO}（主键类型 {@link UUID}）的 CRUD 能力。底层对接 ScyllaDB。
 *
 * @author hanserwei
 * @date 2026/07/08
 * @since 0.0.1
 */
public interface NoteContentRepository extends CassandraRepository<NoteContentDO, UUID> {
}
