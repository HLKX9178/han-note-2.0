package com.hanserwei.note.domain.dataobject;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 笔记收藏表数据对象.
 *
 * <p>对应数据表 {@code t_note_collection}，记录「谁收藏了哪篇笔记」。
 * {@code (user_id, note_id)} 联合唯一索引保证同一用户不能重复收藏同一笔记（幂等）。
 * {@code status} 为业务状态字段（0 取消收藏 / 1 收藏），非框架逻辑删除。
 *
 * @author hanserwei
 * @date 2026/07/11
 * @since 0.0.1
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@TableName("t_note_collection")
public class NoteCollectionDO {

    /** 主键 ID（库自增） */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 用户 ID */
    private Long userId;

    /** 被收藏的笔记 ID */
    private Long noteId;

    /** 收藏时间（用于排序） */
    private LocalDateTime createTime;

    /** 收藏状态（0：取消收藏 1：收藏） */
    private Integer status;
}
