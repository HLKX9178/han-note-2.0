package com.hanserwei.auth.controller;

import com.hanserwei.auth.model.vo.verificationcode.SendVerificationCodeReqVO;
import com.hanserwei.auth.service.VerificationCodeService;
import com.hanserwei.framework.biz.operationlog.aspect.ApiOperationLog;
import com.hanserwei.framework.common.response.Response;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * 短信验证码控制器.
 *
 * <p>对外暴露 {@code POST /verification/code/send} 接口，
 * 供客户端请求短信验证码。无需登录即可访问。
 *
 * @author hanserwei
 * @date 2026/07/07
 * @since 0.0.1
 */
@RestController
@Slf4j
public class VerificationCodeController {

    @Resource
    private VerificationCodeService verificationCodeService;

    /**
     * 发送短信验证码.
     *
     * @param reqVO 请求入参（含手机号）
     * @return 统一响应
     */
    @PostMapping("/verification/code/send")
    @ApiOperationLog(description = "发送短信验证码")
    public Response<?> send(@Validated @RequestBody SendVerificationCodeReqVO reqVO) {
        return verificationCodeService.send(reqVO);
    }
}
