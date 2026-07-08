package com.hanserwei.user.api.dto.resp;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 根据手机号查询用户响应（服务间调用）.
 *
 * <p>仅供内网 RPC 使用，包含 BCrypt 密文与角色标识，切勿经网关对外暴露。
 * 认证服务据此完成密码比对与 JWT 角色声明，无需再读共享缓存。
 *
 * @author hanserwei
 * @date 2026/07/08
 * @since 0.0.1
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FindUserByPhoneRspDTO {

    /** 用户 ID */
    private Long id;

    /** 密码（BCrypt 密文，可能为空——仅验证码注册、未设置过密码） */
    private String password;

    /** 角色标识列表（如 common_user） */
    private List<String> roleKeys;
}
