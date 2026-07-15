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

    private String eventId;
    private Long noteId;
    private Integer delta;
}
