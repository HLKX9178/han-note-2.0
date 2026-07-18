package com.hanserwei.count.api.dto.req;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 查询用户维度计数请求.
 *
 * @author hanserwei
 * @date 2026/07/17
 * @since 0.0.1
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FindUserCountReqDTO {

    /** 用户 ID */
    @NotNull(message = "用户 ID 不能为空")
    private Long userId;
}
