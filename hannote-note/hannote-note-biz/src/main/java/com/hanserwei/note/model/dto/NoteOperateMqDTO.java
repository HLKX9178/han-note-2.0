package com.hanserwei.note.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 笔记操作（发布 / 删除）MQ 消息体.
 *
 * <p>笔记服务发往 {@code NoteOperateTopic}（Tag 区分发布/删除），计数服务消费后更新
 * 用户维度发布笔记数 {@code t_user_count.note_total}。
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

    /** 操作类型：1 发布 / 0 删除 */
    private Integer type;
}
