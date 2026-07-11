package com.hanserwei.note.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 笔记收藏 / 取消收藏 MQ 消息体.
 *
 * <p>笔记服务发往 {@code CollectUnCollectTopic}（Tag 区分收藏/取消），消费者落库
 * {@code t_note_collection} 后转发计数 MQ。{@code noteCreatorId} 用于计数服务同时更新用户维度
 * 获藏数（避免二次回查）。各服务自持一份 DTO，不跨服务共享。
 *
 * @author hanserwei
 * @date 2026/07/11
 * @since 0.0.1
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CollectUnCollectNoteMqDTO {

    /** 操作用户 ID */
    private Long userId;

    /** 笔记 ID */
    private Long noteId;

    /** 操作类型：1 收藏 / 0 取消收藏 */
    private Integer type;

    /** 操作时间 */
    private LocalDateTime createTime;

    /** 笔记发布者 ID（供计数服务更新用户维度获藏数） */
    private Long noteCreatorId;
}
