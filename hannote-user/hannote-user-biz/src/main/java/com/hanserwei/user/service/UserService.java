package com.hanserwei.user.service;

import com.hanserwei.framework.common.response.Response;
import com.hanserwei.user.api.dto.req.FindUserByIdReqDTO;
import com.hanserwei.user.api.dto.req.FindUserByPhoneReqDTO;
import com.hanserwei.user.api.dto.req.FindUsersByIdsReqDTO;
import com.hanserwei.user.api.dto.req.RegisterUserReqDTO;
import com.hanserwei.user.api.dto.req.UpdateUserPasswordReqDTO;
import com.hanserwei.user.api.dto.resp.FindUserByIdRspDTO;
import com.hanserwei.user.api.dto.resp.FindUserByPhoneRspDTO;
import com.hanserwei.user.model.vo.UpdateUserInfoReqVO;

import java.util.List;

/**
 * 用户业务.
 *
 * @author hanserwei
 * @date 2026/07/07
 * @since 0.0.1
 */
public interface UserService {

    /**
     * 更新用户信息.
     *
     * @param updateUserInfoReqVO 修改入参
     * @return 统一响应
     */
    Response<?> updateUserInfo(UpdateUserInfoReqVO updateUserInfoReqVO);

    /**
     * 用户注册（幂等：手机号已注册则直接返回既有用户 ID）.
     *
     * @param registerUserReqDTO 注册入参
     * @return 用户 ID
     */
    Response<Long> register(RegisterUserReqDTO registerUserReqDTO);

    /**
     * 根据手机号查询用户信息（含密文密码与角色标识）.
     *
     * @param findUserByPhoneReqDTO 查询入参
     * @return 用户信息
     */
    Response<FindUserByPhoneRspDTO> findByPhone(FindUserByPhoneReqDTO findUserByPhoneReqDTO);

    /**
     * 更新当前登录用户的密码.
     *
     * <p>userId 从登录上下文获取（由 userId 请求头透传），密文由认证服务传入直接落库。
     *
     * @param updateUserPasswordReqDTO 密码更新入参
     * @return 操作结果
     */
    Response<?> updatePassword(UpdateUserPasswordReqDTO updateUserPasswordReqDTO);

    /**
     * 根据用户 ID 查询用户信息（二级缓存：Caffeine L1 + Redis L2 + DB）.
     *
     * @param findUserByIdReqDTO 查询入参
     * @return 用户信息
     */
    Response<FindUserByIdRspDTO> findById(FindUserByIdReqDTO findUserByIdReqDTO);

    /**
     * 批量根据用户 ID 查询用户信息（Redis multiGet + 回源 DB + pipeline 异步回写）.
     *
     * @param findUsersByIdsReqDTO 批量查询入参（ID 集合大小 [1, 10]）
     * @return 用户信息列表
     */
    Response<List<FindUserByIdRspDTO>> findByIds(FindUsersByIdsReqDTO findUsersByIdsReqDTO);
}
