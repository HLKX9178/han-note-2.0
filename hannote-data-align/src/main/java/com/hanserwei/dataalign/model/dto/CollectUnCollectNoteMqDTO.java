package com.hanserwei.dataalign.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 笔记收藏/取消收藏 MQ 消息体（字段对齐 {@code hannote-count} 的 {@code CountCollectUnCollectNoteMqDTO}，
 * 用于反序列化 {@code CountNoteCollectTopic} 消息）.
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

    /** 被收藏/取消收藏的笔记 ID */
    private Long noteId;

    /** 0：取消收藏，1：收藏 */
    private Integer type;

    /** 操作时间 */
    private LocalDateTime createTime;

    /** 笔记发布者 ID */
    private Long noteCreatorId;
}
