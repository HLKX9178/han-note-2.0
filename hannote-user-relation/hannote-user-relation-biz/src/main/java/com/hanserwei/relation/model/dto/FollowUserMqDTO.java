package com.hanserwei.relation.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 关注操作 MQ 消息体.
 *
 * <p>关注接口发送、消费者落库共用。{@code createTime} 与关注 ZSET 的 score 取自同一时刻，
 * 保证缓存与库时间一致。
 *
 * @author hanserwei
 * @date 2026/07/10
 * @since 0.0.1
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FollowUserMqDTO {

    /** 发起关注的用户 ID */
    private Long userId;

    /** 被关注的用户 ID */
    private Long followUserId;

    /** 关注时间 */
    private LocalDateTime createTime;
}
