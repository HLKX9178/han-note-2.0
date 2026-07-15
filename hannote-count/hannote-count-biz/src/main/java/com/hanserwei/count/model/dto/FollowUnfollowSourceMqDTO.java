package com.hanserwei.count.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 关注/取关源事件消息体（计数服务并行直消费源 Topic 时用于解析）.
 *
 * <p>源 Topic {@code FollowUnfollowTopic} 靠 MQ Tag 区分关注/取关，两种 Tag 的消息体字段不同：
 * 关注体含 {@code followUserId}、取关体含 {@code unfollowUserId}。此 DTO 合并两者字段，
 * 由调用方按 Tag 取对应字段（另一字段为 {@code null}）。
 *
 * @author hanserwei
 * @date 2026/07/14
 * @since 0.0.1
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FollowUnfollowSourceMqDTO {

    /** 发起关注/取关的用户 ID */
    private Long userId;

    /** 被关注用户 ID（Follow Tag 时有值） */
    private Long followUserId;

    /** 被取关用户 ID（Unfollow Tag 时有值） */
    private Long unfollowUserId;

    /** 操作时间 */
    private LocalDateTime createTime;
}
