package com.hanserwei.auth.model.vo.user;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 修改密码请求入参.
 *
 * <p>用户登录后，可通过该接口设置 / 修改登录密码，修改成功后即可使用账号密码方式登录。
 *
 * @author hanserwei
 * @date 2026/07/07
 * @since 0.0.1
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class UpdatePasswordReqVO {

    /** 新密码（明文，服务端以 BCrypt 加密后存储） */
    @NotBlank(message = "新密码不能为空")
    private String newPassword;
}
