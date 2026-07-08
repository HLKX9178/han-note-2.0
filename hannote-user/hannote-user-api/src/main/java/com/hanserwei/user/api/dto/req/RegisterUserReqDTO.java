package com.hanserwei.user.api.dto.req;

import com.hanserwei.framework.common.validator.PhoneNumber;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 用户注册请求（服务间调用）.
 *
 * @author hanserwei
 * @date 2026/07/08
 * @since 0.0.1
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RegisterUserReqDTO {

    /** 手机号 */
    @NotBlank(message = "手机号不能为空")
    @PhoneNumber
    private String phone;
}
