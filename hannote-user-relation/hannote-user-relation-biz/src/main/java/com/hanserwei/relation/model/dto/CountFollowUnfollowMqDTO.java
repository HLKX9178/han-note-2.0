package com.hanserwei.relation.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 计数服务消息体：关注 / 取关.
 *
 * <p>关注/取关落库成功后发送到计数服务，以统计关注数、粉丝数。各服务自持一份，不共享。
 *
 * @author hanserwei
 * @date 2026/07/11
 * @since 0.0.1
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CountFollowUnfollowMqDTO {

    /** 原用户（发起关注/取关的用户） */
    private Long userId;

    /** 目标用户（被关注/被取关的用户） */
    private Long targetUserId;

    /** 操作类型：1 关注，0 取关 */
    private Integer type;
}
