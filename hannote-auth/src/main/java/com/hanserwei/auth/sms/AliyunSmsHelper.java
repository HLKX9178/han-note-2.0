package com.hanserwei.auth.sms;

import com.aliyun.dypnsapi20170525.Client;
import com.aliyun.dypnsapi20170525.models.SendSmsVerifyCodeRequest;
import com.aliyun.dypnsapi20170525.models.SendSmsVerifyCodeResponse;
import com.aliyun.teautil.models.RuntimeOptions;
import com.hanserwei.framework.common.util.JsonUtils;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 阿里云短信认证服务发送工具类.
 *
 * <p>封装 {@link Client} 的调用细节，对外暴露 {@link #sendMessage} 方法，
 * 屏蔽底层 SDK 的请求构造、异常处理与日志输出。采用 fire-and-forget 语义，
 * 发送失败时仅打印错误日志，不向上抛出。
 *
 * @author hanserwei
 * @date 2026/07/07
 * @since 0.0.1
 */
@Component
@Slf4j
public class AliyunSmsHelper {

    @Resource
    private Client client;

    /**
     * 发送短信验证码.
     *
     * @param signName      短信签名
     * @param templateCode  短信模板编码
     * @param phone         接收手机号
     * @param templateParam 模板参数 JSON（如 {@code {"code":"123456","min":"3"}}）
     */
    public void sendMessage(String signName, String templateCode, String phone, String templateParam) {
        SendSmsVerifyCodeRequest request = new SendSmsVerifyCodeRequest()
                .setSignName(signName)
                .setTemplateCode(templateCode)
                .setPhoneNumber(phone)
                .setTemplateParam(templateParam);
        RuntimeOptions runtime = new RuntimeOptions();
        try {
            log.info("==> 开始短信发送, phone: {}, signName: {}, templateCode: {}, templateParam: {}",
                    phone, signName, templateCode, templateParam);
            SendSmsVerifyCodeResponse response = client.sendSmsVerifyCodeWithOptions(request, runtime);
            log.info("==> 短信发送成功, response: {}", JsonUtils.toJsonString(response));
        } catch (Exception error) {
            log.error("==> 短信发送错误: ", error);
        }
    }
}
