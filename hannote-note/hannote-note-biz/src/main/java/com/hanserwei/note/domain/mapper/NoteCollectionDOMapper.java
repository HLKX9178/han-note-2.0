package com.hanserwei.note.domain.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.hanserwei.note.domain.dataobject.NoteCollectionDO;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 笔记收藏表 Mapper.
 *
 * @author hanserwei
 * @date 2026/07/11
 * @since 0.0.1
 */
public interface NoteCollectionDOMapper extends BaseMapper<NoteCollectionDO> {

    /**
     * 新增或更新收藏记录（PostgreSQL upsert，SQL 见 NoteCollectionDOMapper.xml）.
     *
     * <p>命中 {@code (user_id, note_id)} 唯一索引时更新 {@code status} 与 {@code create_time}。
     * {@code WHERE t_note_collection.status <> EXCLUDED.status} 幂等守卫：状态未变化（如 MQ 重复
     * 投递同一次收藏）时不更新、影响行数为 0，避免消费端重复转发计数导致重复 +1。
     *
     * @param noteCollectionDO 收藏记录（status 传 1 表示收藏）
     * @return 影响行数（1 表示确实发生了收藏状态变更）
     */
    int insertOrUpdateCollect(NoteCollectionDO noteCollectionDO);

    /**
     * 批量新增或更新收藏记录（PostgreSQL 批量 upsert）.
     *
     * <p>命中 {@code (user_id, note_id)} 唯一索引时更新 {@code status} 与 {@code create_time}；
     * {@code WHERE t_note_collection.status <> EXCLUDED.status} 幂等守卫，状态未变化不更新。
     * 供批量顺序消费的收藏消费者对内存合并后的最终操作一次落库。
     *
     * @param list 合并后的收藏/取消收藏记录（status 为 1 收藏 / 0 取消）
     * @return 实际影响行数
     */
    int batchInsertOrUpdate(@Param("list") List<NoteCollectionDO> list);
}
