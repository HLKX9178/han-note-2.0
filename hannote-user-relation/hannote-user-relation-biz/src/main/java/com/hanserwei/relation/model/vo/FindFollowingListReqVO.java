package com.hanserwei.relation.model.vo;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 查询关注列表请求.
 *
 * @author hanserwei
 * @date 2026/07/10
 * @since 0.0.1
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class FindFollowingListReqVO {

    /** 想要查询的用户 ID */
    @NotNull(message = "查询用户 ID 不能为空")
    private Long userId;

    /** 当前页码（默认第一页） */
    @NotNull(message = "页码不能为空")
    private Integer pageNo = 1;
}
