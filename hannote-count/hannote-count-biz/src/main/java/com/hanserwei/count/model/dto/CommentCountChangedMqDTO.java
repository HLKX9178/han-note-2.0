package com.hanserwei.count.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 笔记评论总数变更源消息.
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

    /** 事件 ID：评论侧生成的幂等标识，用于去重/追溯 */
    private String eventId;

    /** 笔记 ID */
    private Long noteId;

    /** 评论数有符号增量：新增评论 +1，删除评论 -1 */
    private Integer delta;
}
