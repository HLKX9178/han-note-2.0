package com.hanserwei.note.domain.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.hanserwei.note.domain.dataobject.NoteDO;
import com.hanserwei.note.api.dto.resp.FindPublishedNoteRspDTO;
import org.apache.ibatis.annotations.Param;

import java.util.List;

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

    /**
     * 查询正常发布中的笔记最小信息.
     *
     * @param noteId 笔记 ID
     * @return 笔记最小信息
     */
    FindPublishedNoteRspDTO selectPublishedById(@Param("noteId") Long noteId);

    /**
     * 游标分页查询指定博主已发布的笔记列表（公开 + 正常展示，按笔记 ID 降序，每页 20 条）.
     *
     * @param creatorId 博主用户 ID
     * @param cursor    游标（笔记 ID）；为 {@code null} 查询第一页
     * @return 笔记列表（最多 20 条）
     */
    List<NoteDO> selectPublishedNoteListByUserIdAndCursor(@Param("creatorId") Long creatorId,
                                                          @Param("cursor") Long cursor);

    /**
     * 统计指定笔记 ID 的记录数（供发布事务消息回查本地事务状态）.
     *
     * @param noteId 笔记 ID
     * @return 记录数（存在返回 1，否则 0）
     */
    int selectCountByNoteId(@Param("noteId") Long noteId);
}
