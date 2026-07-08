package com.hanserwei.auth.rpc;

import com.hanserwei.framework.common.response.Response;
import com.hanserwei.user.api.UserHttpApi;
import com.hanserwei.user.api.dto.req.FindUserByPhoneReqDTO;
import com.hanserwei.user.api.dto.req.RegisterUserReqDTO;
import com.hanserwei.user.api.dto.req.UpdateUserPasswordReqDTO;
import com.hanserwei.user.api.dto.resp.FindUserByPhoneRspDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Objects;

/**
 * 用户服务 RPC 调用封装.
 *
 * <p>对 {@link UserHttpApi} 的薄封装：解包 {@code Response<T>}，屏蔽 HTTP Interface 细节。
 *
 * @author hanserwei
 * @date 2026/07/08
 * @since 0.0.1
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class UserRpcService {

    private final UserHttpApi userHttpApi;

    /**
     * 注册用户（幂等）.
     *
     * @param phone 手机号
     * @return 用户 ID；失败返回 {@code null}
     */
    public Long registerUser(String phone) {
        RegisterUserReqDTO reqDTO = RegisterUserReqDTO.builder().phone(phone).build();
        Response<Long> response = userHttpApi.register(reqDTO);
        if (Objects.isNull(response) || !response.isSuccess()) {
            return null;
        }
        return response.getData();
    }

    /**
     * 根据手机号查询用户信息.
     *
     * @param phone 手机号
     * @return 用户信息（含密文密码与角色）；不存在或失败返回 {@code null}
     */
    public FindUserByPhoneRspDTO findUserByPhone(String phone) {
        FindUserByPhoneReqDTO reqDTO = FindUserByPhoneReqDTO.builder().phone(phone).build();
        Response<FindUserByPhoneRspDTO> response = userHttpApi.findByPhone(reqDTO);
        if (Objects.isNull(response) || !response.isSuccess()) {
            return null;
        }
        return response.getData();
    }

    /**
     * 更新密码（userId 由 userId 请求头透传，无需显式传参）.
     *
     * @param encodePassword BCrypt 密文
     */
    public void updatePassword(String encodePassword) {
        UpdateUserPasswordReqDTO reqDTO = UpdateUserPasswordReqDTO.builder()
                .encodePassword(encodePassword)
                .build();
        userHttpApi.updatePassword(reqDTO);
    }
}
