package com.hanserwei.comment.model.dto;

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

    /** 事件 ID（幂等去重用） */
    private String eventId;
    /** 评论数发生变更的笔记 ID */
    private Long noteId;
    /** 评论数增量（正数新增、负数删除） */
    private Integer delta;
}
