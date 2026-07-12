package com.hanserwei.dataalign.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 用户关注/取关 MQ 消息体（字段对齐 {@code hannote-count} 的 {@code CountFollowUnfollowMqDTO}，
 * 用于反序列化 {@code CountFollowingTopic} 消息）.
 *
 * @author hanserwei
 * @date 2026/07/11
 * @since 0.0.1
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FollowUnfollowMqDTO {

    /** 源用户 ID（发起关注/取关的一方，其关注数变更） */
    private Long userId;

    /** 目标用户 ID（被关注/被取关的一方，其粉丝数变更） */
    private Long targetUserId;

    /** 1：关注，0：取关 */
    private Integer type;
}
