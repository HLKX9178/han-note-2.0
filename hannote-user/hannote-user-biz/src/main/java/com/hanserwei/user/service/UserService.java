package com.hanserwei.user.service;

import com.hanserwei.framework.common.response.Response;
import com.hanserwei.user.model.vo.UpdateUserInfoReqVO;

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
}
