package com.hanserwei.auth.model.vo.verificationcode;

import com.hanserwei.framework.common.validator.PhoneNumber;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 发送短信验证码请求入参.
 *
 * @author hanserwei
 * @date 2026/07/07
 * @since 0.0.1
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class SendVerificationCodeReqVO {

    /** 手机号（需为 11 位数字） */
    @NotBlank(message = "手机号不能为空")
    @PhoneNumber
    private String phone;
}
