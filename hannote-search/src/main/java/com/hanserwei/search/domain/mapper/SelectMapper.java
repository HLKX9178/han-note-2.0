package com.hanserwei.search.domain.mapper;

import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

/**
 * ES 索引数据查询 Mapper.
 *
 * <p>增量同步时按 noteId / userId 从 PostgreSQL（{@code t_note} / {@code t_user} /
 * {@code t_note_count} / {@code t_user_count}，三表同库）重查笔记 / 用户索引所需的全字段，
 * 返回 {@code Map}（key 即 ES 索引字段名，直接落库，通用性更强）。SQL 与
 * {@code scripts/es-index/full_build.sh} 完全一致（PostgreSQL 方言）。
 *
 * @author hanserwei
 * @date 2026/07/13
 * @since 0.0.1
 */
public interface SelectMapper {

    /**
     * 查询笔记文档所需的全字段数据（可按 noteId 单条、或按 userId 查该用户全部笔记）。
     *
     * @param noteId 笔记 ID，为 {@code null} 时不追加此条件
     * @param userId 发布者用户 ID，为 {@code null} 时不追加此条件
     * @return 每行一个 Map（key 为 ES 索引字段名）
     */
    List<Map<String, Object>> selectEsNoteIndexData(@Param("noteId") Long noteId, @Param("userId") Long userId);

    /**
     * 查询用户文档所需的全字段数据。
     *
     * @param userId 用户 ID
     * @return 每行一个 Map（key 为 ES 索引字段名）
     */
    List<Map<String, Object>> selectEsUserIndexData(@Param("userId") Long userId);
}
