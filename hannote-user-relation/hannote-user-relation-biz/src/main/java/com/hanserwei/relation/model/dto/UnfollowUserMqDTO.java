package com.hanserwei.relation.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 取关操作 MQ 消息体.
 *
 * <p>取关接口发送、消费者删库共用。消费者据此删除 t_following/t_fans 两条记录，
 * 并从被取关方粉丝 ZSET 移除发起者。
 *
 * @author hanserwei
 * @date 2026/07/10
 * @since 0.0.1
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UnfollowUserMqDTO {

    /** 发起取关的用户 ID */
    private Long userId;

    /** 被取关的用户 ID */
    private Long unfollowUserId;

    /** 取关时间 */
    private LocalDateTime createTime;
}
