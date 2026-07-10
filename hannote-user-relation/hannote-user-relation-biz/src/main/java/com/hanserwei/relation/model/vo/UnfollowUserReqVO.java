package com.hanserwei.relation.model.vo;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 取关用户请求.
 *
 * @author hanserwei
 * @date 2026/07/10
 * @since 0.0.1
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UnfollowUserReqVO {

    /** 被取关的用户 ID */
    @NotNull(message = "被取关用户 ID 不能为空")
    private Long unfollowUserId;
}
