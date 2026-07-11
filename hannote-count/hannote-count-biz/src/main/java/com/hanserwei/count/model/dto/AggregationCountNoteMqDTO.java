package com.hanserwei.count.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 聚合后的笔记计数消息体（点赞 / 收藏落库共用）.
 *
 * <p>计数消费者按 noteId 分组净算增量后产出，携带笔记发布者 ID，供落库消费者同时更新
 * 笔记维度（{@code t_note_count}）与用户维度（{@code t_user_count}）两张表。
 *
 * @author hanserwei
 * @date 2026/07/11
 * @since 0.0.1
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AggregationCountNoteMqDTO {

    /** 笔记发布者 ID */
    private Long creatorId;

    /** 笔记 ID */
    private Long noteId;

    /** 聚合后的净增量（可正可负） */
    private Integer count;
}
