package com.hanserwei.auth.model.vo.user;

import com.hanserwei.framework.common.validator.PhoneNumber;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 用户登录请求入参.
 *
 * <p>同时支持两种登录方式：
 * <ul>
 *   <li>{@code type = 1}：手机号 + 验证码（{@code code} 必填）；</li>
 *   <li>{@code type = 2}：账号 + 密码（{@code password} 必填，阶段一未启用）。</li>
 * </ul>
 *
 * @author hanserwei
 * @date 2026/07/07
 * @since 0.0.1
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class UserLoginReqVO {

    /** 手机号（需为 11 位数字） */
    @NotBlank(message = "手机号不能为空")
    @PhoneNumber
    private String phone;

    /** 验证码（验证码登录时填写） */
    private String code;

    /** 密码（密码登录时填写） */
    private String password;

    /** 登录类型：1 验证码；2 密码 */
    @NotNull(message = "登录类型不能为空")
    private Integer type;
}
