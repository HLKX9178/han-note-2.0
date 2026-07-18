package com.hanserwei.user.controller;

import com.hanserwei.framework.biz.operationlog.aspect.ApiOperationLog;
import com.hanserwei.framework.common.response.Response;
import com.hanserwei.user.api.dto.req.FindUserByIdReqDTO;
import com.hanserwei.user.api.dto.req.FindUserByPhoneReqDTO;
import com.hanserwei.user.api.dto.req.FindUsersByIdsReqDTO;
import com.hanserwei.user.api.dto.req.RegisterUserReqDTO;
import com.hanserwei.user.api.dto.req.UpdateUserPasswordReqDTO;
import com.hanserwei.user.api.dto.resp.FindUserByIdRspDTO;
import com.hanserwei.user.api.dto.resp.FindUserByPhoneRspDTO;
import com.hanserwei.user.model.vo.FindUserProfileReqVO;
import com.hanserwei.user.model.vo.FindUserProfileRspVO;
import com.hanserwei.user.model.vo.UpdateUserInfoReqVO;
import com.hanserwei.user.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 用户控制器.
 *
 * @author hanserwei
 * @date 2026/07/07
 * @since 0.0.1
 */
@Slf4j
@RestController
@RequestMapping("/user")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    /**
     * 修改用户信息（multipart 表单，含头像/背景图上传）.
     *
     * <p>注意：本接口含文件流入参，禁止标注 {@code @ApiOperationLog}（序列化文件流会出问题）。
     *
     * @param updateUserInfoReqVO 修改入参
     * @return 统一响应
     */
    @PostMapping(value = "/update", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Response<?> updateUserInfo(UpdateUserInfoReqVO updateUserInfoReqVO) {
        return userService.updateUserInfo(updateUserInfoReqVO);
    }

    /**
     * 获取用户主页信息（经网关，白名单放行，匿名可查看他人主页）.
     *
     * @param findUserProfileReqVO 入参（userId 可空）
     * @return 主页信息
     */
    @PostMapping("/profile")
    @ApiOperationLog(description = "获取用户主页信息")
    public Response<FindUserProfileRspVO> findUserProfile(@Validated @RequestBody FindUserProfileReqVO findUserProfileReqVO) {
        return userService.findUserProfile(findUserProfileReqVO);
    }

    // ===================================== 对其他服务提供的接口（仅内网 RPC） =====================================

    /**
     * 用户注册（供认证服务在自动注册时 RPC 调用）.
     *
     * @param registerUserReqDTO 注册入参
     * @return 用户 ID
     */
    @PostMapping("/register")
    @ApiOperationLog(description = "用户注册")
    public Response<Long> register(@Validated @RequestBody RegisterUserReqDTO registerUserReqDTO) {
        return userService.register(registerUserReqDTO);
    }

    /**
     * 根据手机号查询用户信息（供认证服务密码登录 RPC 调用）.
     *
     * @param findUserByPhoneReqDTO 查询入参
     * @return 用户信息（含密文密码与角色）
     */
    @PostMapping("/findByPhone")
    @ApiOperationLog(description = "手机号查询用户信息")
    public Response<FindUserByPhoneRspDTO> findByPhone(@Validated @RequestBody FindUserByPhoneReqDTO findUserByPhoneReqDTO) {
        return userService.findByPhone(findUserByPhoneReqDTO);
    }

    /**
     * 更新密码（供认证服务修改密码 RPC 调用，userId 由请求头透传）.
     *
     * @param updateUserPasswordReqDTO 密码更新入参
     * @return 操作结果
     */
    @PostMapping("/password/update")
    @ApiOperationLog(description = "密码更新")
    public Response<?> updatePassword(@Validated @RequestBody UpdateUserPasswordReqDTO updateUserPasswordReqDTO) {
        return userService.updatePassword(updateUserPasswordReqDTO);
    }

    /**
     * 根据用户 ID 查询用户信息（供笔记详情等场景 RPC 调用，二级缓存加持）.
     *
     * @param findUserByIdReqDTO 查询入参
     * @return 用户信息（ID / 昵称 / 头像）
     */
    @PostMapping("/findById")
    @ApiOperationLog(description = "查询用户信息")
    public Response<FindUserByIdRspDTO> findById(@Validated @RequestBody FindUserByIdReqDTO findUserByIdReqDTO) {
        return userService.findById(findUserByIdReqDTO);
    }

    /**
     * 批量查询用户信息（供关注/粉丝列表等场景 RPC 调用，缓存 + pipeline 回写加持）.
     *
     * @param findUsersByIdsReqDTO 批量查询入参（ID 集合大小 [1, 10]）
     * @return 用户信息列表
     */
    @PostMapping("/findByIds")
    @ApiOperationLog(description = "批量查询用户信息")
    public Response<List<FindUserByIdRspDTO>> findByIds(@Validated @RequestBody FindUsersByIdsReqDTO findUsersByIdsReqDTO) {
        return userService.findByIds(findUsersByIdsReqDTO);
    }
}
