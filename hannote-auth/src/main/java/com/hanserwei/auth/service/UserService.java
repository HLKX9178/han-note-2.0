package com.hanserwei.auth.service;

import com.hanserwei.auth.model.vo.user.UserLoginReqVO;
import com.hanserwei.framework.common.response.Response;

/**
 * 用户业务接口.
 *
 * @author hanserwei
 * @date 2026/07/07
 * @since 0.0.1
 */
public interface UserService {

    /**
     * 登录（新用户自动注册）.
     *
     * @param reqVO 登录请求入参
     * @return 登录成功返回 JWT 字符串
     */
    Response<?> loginAndRegister(UserLoginReqVO reqVO);
}
