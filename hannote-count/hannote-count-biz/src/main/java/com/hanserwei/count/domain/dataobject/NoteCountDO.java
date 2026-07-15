package com.hanserwei.count.domain.dataobject;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 笔记计数表数据对象.
 *
 * <p>对应数据表 {@code t_note_count}。按笔记维度维护点赞、收藏、评论计数，
 * 随点赞/收藏/评论动作增量 {@code +1}/{@code -1}，避免实时 {@code COUNT} 聚合查询。
 *
 * @author hanserwei
 * @date 2026/07/11
 * @since 0.0.1
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@TableName("t_note_count")
public class NoteCountDO {

    /** 主键 ID（库自增） */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 笔记 ID */
    private Long noteId;

    /** 获得点赞总数 */
    private Long likeTotal;

    /** 获得收藏总数 */
    private Long collectTotal;

    /** 被评论总数 */
    private Long commentTotal;
}
