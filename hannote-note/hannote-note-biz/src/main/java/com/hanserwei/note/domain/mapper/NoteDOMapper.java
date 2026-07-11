package com.hanserwei.note.domain.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.hanserwei.note.domain.dataobject.NoteDO;
import org.apache.ibatis.annotations.Param;

/**
 * 笔记表 Mapper.
 *
 * @author hanserwei
 * @date 2026/07/09
 * @since 0.0.1
 */
public interface NoteDOMapper extends BaseMapper<NoteDO> {

    /**
     * 查询正常展示中的笔记发布者 ID（status = 1）.
     *
     * @param noteId 笔记 ID
     * @return 发布者用户 ID；笔记不存在返回 {@code null}
     */
    Long selectCreatorIdByNoteId(@Param("noteId") Long noteId);
}
