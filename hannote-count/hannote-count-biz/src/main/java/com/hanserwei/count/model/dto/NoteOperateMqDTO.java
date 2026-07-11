package com.hanserwei.count.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 笔记操作（发布 / 删除）计数 MQ 消息体（计数服务侧，与笔记服务镜像）.
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
