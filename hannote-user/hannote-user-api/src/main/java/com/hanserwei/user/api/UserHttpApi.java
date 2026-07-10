package com.hanserwei.user.api;

import com.hanserwei.framework.common.response.Response;
import com.hanserwei.user.api.dto.req.FindUserByIdReqDTO;
import com.hanserwei.user.api.dto.req.FindUserByPhoneReqDTO;
import com.hanserwei.user.api.dto.req.FindUsersByIdsReqDTO;
import com.hanserwei.user.api.dto.req.RegisterUserReqDTO;
import com.hanserwei.user.api.dto.req.UpdateUserPasswordReqDTO;
import com.hanserwei.user.api.dto.resp.FindUserByIdRspDTO;
import com.hanserwei.user.api.dto.resp.FindUserByPhoneRspDTO;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.service.annotation.HttpExchange;
import org.springframework.web.service.annotation.PostExchange;

import java.util.List;

/**
 * 用户服务对外契约（HTTP Interface）.
 *
 * <p>供其他服务通过 {@code @ImportHttpServices(group = UserApiConstants.SERVICE_NAME,
 * types = UserHttpApi.class)} 声明并注入调用。这些接口仅供内网服务间 RPC 使用，
 * 不经网关对外暴露。
 *
 * @author hanserwei
 * @date 2026/07/08
 * @since 0.0.1
 */
@HttpExchange
public interface UserHttpApi {

    /** 用户服务上下文前缀（与 UserController 的 @RequestMapping 对齐） */
    String PREFIX = "/user";

    /**
     * 用户注册（幂等：手机号已注册则直接返回既有用户 ID）.
     *
     * @param registerUserReqDTO 注册入参
     * @return 用户 ID
     */
    @PostExchange(PREFIX + "/register")
    Response<Long> register(@RequestBody RegisterUserReqDTO registerUserReqDTO);

    /**
     * 根据手机号查询用户信息（含密文密码与角色标识）.
     *
     * @param findUserByPhoneReqDTO 查询入参
     * @return 用户信息
     */
    @PostExchange(PREFIX + "/findByPhone")
    Response<FindUserByPhoneRspDTO> findByPhone(@RequestBody FindUserByPhoneReqDTO findUserByPhoneReqDTO);

    /**
     * 更新当前登录用户的密码（userId 由 userId 请求头透传）.
     *
     * @param updateUserPasswordReqDTO 密码更新入参
     * @return 操作结果
     */
    @PostExchange(PREFIX + "/password/update")
    Response<?> updatePassword(@RequestBody UpdateUserPasswordReqDTO updateUserPasswordReqDTO);

    /**
     * 根据用户 ID 查询用户信息（供笔记详情等场景 RPC 调用）.
     *
     * @param findUserByIdReqDTO 查询入参
     * @return 用户信息（ID / 昵称 / 头像）
     */
    @PostExchange(PREFIX + "/findById")
    Response<FindUserByIdRspDTO> findById(@RequestBody FindUserByIdReqDTO findUserByIdReqDTO);

    /**
     * 批量根据用户 ID 查询用户信息（供关注/粉丝列表等场景 RPC 调用）.
     *
     * @param findUsersByIdsReqDTO 批量查询入参（ID 集合大小 [1, 10]）
     * @return 用户信息列表（ID / 昵称 / 头像 / 简介）
     */
    @PostExchange(PREFIX + "/findByIds")
    Response<List<FindUserByIdRspDTO>> findByIds(@RequestBody FindUsersByIdsReqDTO findUsersByIdsReqDTO);
}
