package com.hanserwei.user.controller;

import com.hanserwei.framework.common.response.Response;
import com.hanserwei.user.model.vo.UpdateUserInfoReqVO;
import com.hanserwei.user.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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
}
