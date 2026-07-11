package com.hanserwei.count.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 笔记点赞 / 取消点赞计数 MQ 消息体（计数服务侧，与笔记服务镜像）.
 *
 * @author hanserwei
 * @date 2026/07/11
 * @since 0.0.1
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CountLikeUnlikeNoteMqDTO {

    /** 操作用户 ID */
    private Long userId;

    /** 笔记 ID */
    private Long noteId;

    /** 操作类型：1 点赞 / 0 取消点赞 */
    private Integer type;

    /** 操作时间 */
    private LocalDateTime createTime;

    /** 笔记发布者 ID（用于更新用户维度获赞数） */
    private Long noteCreatorId;
}
