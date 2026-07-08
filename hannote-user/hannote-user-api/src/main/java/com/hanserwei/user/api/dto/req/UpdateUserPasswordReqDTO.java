package com.hanserwei.user.api.dto.req;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 更新用户密码请求（服务间调用）.
 *
 * <p>密码由认证服务 BCrypt 加密后传入，用户服务直接落库，不再二次加密。
 *
 * @author hanserwei
 * @date 2026/07/08
 * @since 0.0.1
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateUserPasswordReqDTO {

    /** 加密后的密码（BCrypt 密文） */
    @NotBlank(message = "密码不能为空")
    private String encodePassword;
}
