package com.hanserwei.dataalign.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 笔记操作（发布/删除）MQ 消息体（字段对齐 {@code hannote-count} 的 {@code NoteOperateMqDTO}，
 * 用于反序列化 {@code NoteOperateTopic} 消息）.
 *
 * @author hanserwei
 * @date 2026/07/11
 * @since 0.0.1
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NoteOperateMqDTO {

    /** 笔记发布者 ID */
    private Long creatorId;

    /** 笔记 ID */
    private Long noteId;

    /** 0：笔记删除，1：笔记发布 */
    private Integer type;
}
