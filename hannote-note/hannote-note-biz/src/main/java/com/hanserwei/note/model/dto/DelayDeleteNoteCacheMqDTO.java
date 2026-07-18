package com.hanserwei.note.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 延迟双删 Redis 笔记缓存消息体.
 *
 * <p>携带笔记 ID 与作者 ID，供消费者二次删除「笔记详情缓存」与「作者已发布笔记列表缓存」。
 *
 * @author hanserwei
 * @date 2026/07/18
 * @since 0.0.1
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DelayDeleteNoteCacheMqDTO {

    /** 笔记 ID（用于删除笔记详情缓存） */
    private Long noteId;

    /** 作者用户 ID（用于删除已发布笔记列表缓存） */
    private Long userId;
}
