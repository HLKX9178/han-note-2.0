package com.hanserwei.user.api.dto.req;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 根据用户 ID 查询用户信息请求（服务间调用）.
 *
 * @author hanserwei
 * @date 2026/07/09
 * @since 0.0.1
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FindUserByIdReqDTO {

    /** 用户 ID */
    @NotNull(message = "用户 ID 不能为空")
    private Long id;
}
