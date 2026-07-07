package com.hanserwei.auth.service;

import com.hanserwei.auth.model.vo.verificationcode.SendVerificationCodeReqVO;
import com.hanserwei.framework.common.response.Response;

/**
 * 短信验证码业务接口.
 *
 * @author hanserwei
 * @date 2026/07/07
 * @since 0.0.1
 */
public interface VerificationCodeService {

    /**
     * 发送短信验证码
     */
    Response<?> send(SendVerificationCodeReqVO reqVO);
}
