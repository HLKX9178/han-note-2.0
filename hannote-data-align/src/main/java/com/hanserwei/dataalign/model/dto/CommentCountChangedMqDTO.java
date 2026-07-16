package com.hanserwei.dataalign.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 笔记评论总数变更消息.
 *
 * @author hanserwei
 * @date 2026/07/15
 * @since 0.0.1
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CommentCountChangedMqDTO {

    /** 事件唯一 ID，用于消费端幂等去重 */
    private String eventId;
    /** 发生评论总数变更的笔记 ID */
    private Long noteId;
    /** 评论数变化量（新增为正、删除为负） */
    private Integer delta;
}
