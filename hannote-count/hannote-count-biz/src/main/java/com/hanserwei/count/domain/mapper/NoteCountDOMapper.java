package com.hanserwei.count.domain.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.hanserwei.count.domain.dataobject.NoteCountDO;
import org.apache.ibatis.annotations.Param;

/**
 * 笔记计数表 Mapper.
 *
 * @author hanserwei
 * @date 2026/07/11
 * @since 0.0.1
 */
public interface NoteCountDOMapper extends BaseMapper<NoteCountDO> {

    /**
     * 新增或累加笔记点赞总数（PG upsert）。
     *
     * @param count  本批次点赞数增量（可正可负）
     * @param noteId 笔记 ID
     * @return 受影响行数
     */
    int insertOrUpdateLikeTotalByNoteId(@Param("count") Integer count, @Param("noteId") Long noteId);

    /**
     * 新增或累加笔记收藏总数（PG upsert）。
     *
     * @param count  本批次收藏数增量（可正可负）
     * @param noteId 笔记 ID
     * @return 受影响行数
     */
    int insertOrUpdateCollectTotalByNoteId(@Param("count") Integer count, @Param("noteId") Long noteId);
}
