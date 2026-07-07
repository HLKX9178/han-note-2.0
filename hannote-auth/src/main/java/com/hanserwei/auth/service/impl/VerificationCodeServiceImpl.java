package com.hanserwei.auth.service.impl;

import cn.hutool.core.util.RandomUtil;
import com.hanserwei.auth.constant.RedisKeyConstants;
import com.hanserwei.auth.enums.ResponseCodeEnum;
import com.hanserwei.auth.model.vo.verificationcode.SendVerificationCodeReqVO;
import com.hanserwei.auth.service.VerificationCodeService;
import com.hanserwei.auth.sms.AliyunAccessKeyProperties;
import com.hanserwei.auth.sms.AliyunSmsHelper;
import com.hanserwei.framework.common.exception.BizException;
import com.hanserwei.framework.common.response.Response;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.task.TaskExecutor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

/**
 * 短信验证码业务实现.
 *
 * <p>发送流程：
 * <ol>
 *   <li>校验同一手机号 3 分钟内是否已发送（Redis Key 未过期视为请求频繁）；</li>
 *   <li>生成 6 位数字验证码；</li>
 *   <li>通过虚拟线程池异步调用阿里云短信认证服务发送验证码；</li>
 *   <li>将验证码缓存到 Redis 并设置 3 分钟 TTL。</li>
 * </ol>
 *
 * @author hanserwei
 * @date 2026/07/07
 * @since 0.0.1
 */
@Service
@Slf4j
public class VerificationCodeServiceImpl implements VerificationCodeService {

    /** 验证码有效期（分钟） */
    private static final int CODE_TTL_MINUTES = 3;

    @Resource
    private RedisTemplate<String, Object> redisTemplate;

    @Resource(name = "taskExecutor")
    private TaskExecutor taskExecutor;

    @Resource
    private AliyunSmsHelper aliyunSmsHelper;

    @Resource
    private AliyunAccessKeyProperties aliyunAccessKeyProperties;

    /**
     * 发送短信验证码.
     *
     * @param reqVO 请求入参（含手机号）
     * @return 成功时返回 {@code success=true}
     * @throws BizException 请求太频繁时抛出 {@link ResponseCodeEnum#VERIFICATION_CODE_SEND_FREQUENTLY}
     */
    @Override
    public Response<?> send(SendVerificationCodeReqVO reqVO) {
        String phone = reqVO.getPhone();
        String key = RedisKeyConstants.buildVerificationCodeKey(phone);

        // 判断是否已发送（未过期则视为请求频繁）
        boolean isSent = Boolean.TRUE.equals(redisTemplate.hasKey(key));
        if (isSent) {
            throw new BizException(ResponseCodeEnum.VERIFICATION_CODE_SEND_FREQUENTLY);
        }

        // 生成 6 位数字验证码
        String verificationCode = RandomUtil.randomNumbers(6);
        log.info("==> 手机号: {}, 已生成验证码：【{}】", phone, verificationCode);

        // 异步调用阿里云短信认证服务发送验证码
        taskExecutor.execute(() -> {
            String signName = aliyunAccessKeyProperties.getSms().getSignName();
            String templateCode = aliyunAccessKeyProperties.getSms().getTemplateCode();
            String templateParam = String.format("{\"code\":\"%s\",\"min\":\"%d\"}",
                    verificationCode, CODE_TTL_MINUTES);
            aliyunSmsHelper.sendMessage(signName, templateCode, phone, templateParam);
        });

        // 缓存验证码并设置过期时间
        redisTemplate.opsForValue().set(key, verificationCode, Duration.ofMinutes(CODE_TTL_MINUTES));
        return Response.success();
    }
}
