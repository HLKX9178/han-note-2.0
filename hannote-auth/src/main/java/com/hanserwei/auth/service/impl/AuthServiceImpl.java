package com.hanserwei.auth.service.impl;

import com.google.common.base.Preconditions;
import com.hanserwei.auth.constant.AuthConstants;
import com.hanserwei.auth.constant.RedisKeyConstants;
import com.hanserwei.auth.enums.LoginTypeEnum;
import com.hanserwei.auth.enums.ResponseCodeEnum;
import com.hanserwei.auth.model.vo.user.UpdatePasswordReqVO;
import com.hanserwei.auth.model.vo.user.UserLoginReqVO;
import com.hanserwei.auth.rpc.UserRpcService;
import com.hanserwei.auth.security.HannoteUserDetails;
import com.hanserwei.auth.security.JwtTokenProvider;
import com.hanserwei.auth.service.AuthService;
import com.hanserwei.framework.biz.context.holder.LoginUserContextHolder;
import com.hanserwei.framework.common.exception.BizException;
import com.hanserwei.framework.common.response.Response;
import com.hanserwei.user.api.dto.resp.FindUserByPhoneRspDTO;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

/**
 * 认证业务实现.
 *
 * <p>提供登录（新用户自动注册）能力，支持验证码 / 密码两种登录方式。
 * 用户数据的读写（注册、按手机号查询、密码更新）通过 {@link UserRpcService}
 * RPC 调用用户服务完成，本服务不再直接操作数据库。
 *
 * @author hanserwei
 * @date 2026/07/07
 * @since 0.0.1
 */
@Service
@Slf4j
public class AuthServiceImpl implements AuthService {

    @Resource
    private RedisTemplate<String, Object> redisTemplate;

    @Resource
    private JwtTokenProvider jwtTokenProvider;

    @Resource
    private PasswordEncoder passwordEncoder;

    @Resource
    private UserRpcService userRpcService;

    /**
     * 登录（新用户自动注册）.
     *
     * <p>流程：
     * <ol>
     *   <li>校验登录类型；</li>
     *   <li>验证码登录：校验验证码 → 未注册则 RPC 自动注册 → 签发 JWT；</li>
     *   <li>密码登录：RPC 查询用户 → BCrypt 比对密码 → 签发 JWT。</li>
     * </ol>
     *
     * @param reqVO 登录请求入参
     * @return 登录成功返回 JWT 字符串
     * @throws BizException 验证码错误、登录类型不支持、参数校验失败时抛出
     */
    @Override
    public Response<?> loginAndRegister(UserLoginReqVO reqVO) {
        String phone = reqVO.getPhone();
        LoginTypeEnum loginTypeEnum = LoginTypeEnum.of(reqVO.getType());
        Preconditions.checkArgument(loginTypeEnum != null, "登录类型不能为空");

        Long userId;
        List<String> roleKeys;

        switch (loginTypeEnum) {
            case VERIFICATION_CODE:
                String code = reqVO.getCode();
                Preconditions.checkArgument(StringUtils.isNotBlank(code), "验证码不能为空");

                // 校验验证码（一次使用后立即删除，防止重放）
                String redisKey = RedisKeyConstants.buildVerificationCodeKey(phone);
                Object cachedCode = redisTemplate.opsForValue().get(redisKey);
                if (cachedCode == null || !Objects.equals(code, String.valueOf(cachedCode))) {
                    throw new BizException(ResponseCodeEnum.VERIFICATION_CODE_ERROR);
                }
                redisTemplate.delete(redisKey);

                // RPC 查询用户是否已注册
                FindUserByPhoneRspDTO existed = userRpcService.findUserByPhone(phone);
                if (Objects.isNull(existed)) {
                    // 新用户：RPC 自动注册
                    userId = userRpcService.registerUser(phone);
                    if (Objects.isNull(userId)) {
                        throw new BizException(ResponseCodeEnum.LOGIN_FAIL);
                    }
                    roleKeys = List.of(AuthConstants.COMMON_USER_ROLE_KEY);
                } else {
                    userId = existed.getId();
                    roleKeys = resolveRoleKeys(existed);
                }
                break;

            case PASSWORD:
                String password = reqVO.getPassword();
                Preconditions.checkArgument(StringUtils.isNotBlank(password), "密码不能为空");

                // RPC 查询用户
                FindUserByPhoneRspDTO userRsp = userRpcService.findUserByPhone(phone);
                if (Objects.isNull(userRsp)) {
                    throw new BizException(ResponseCodeEnum.USER_NOT_FOUND);
                }

                // 比对明文密码与库中 BCrypt 密文
                if (StringUtils.isBlank(userRsp.getPassword())
                        || !passwordEncoder.matches(password, userRsp.getPassword())) {
                    throw new BizException(ResponseCodeEnum.PHONE_OR_PASSWORD_ERROR);
                }

                userId = userRsp.getId();
                roleKeys = resolveRoleKeys(userRsp);
                break;

            default:
                throw new BizException(ResponseCodeEnum.LOGIN_TYPE_NOT_SUPPORT);
        }

        // 签发 JWT
        String token = jwtTokenProvider.generateToken(userId, phone, roleKeys);
        return Response.success(token);
    }

    /**
     * 修改密码.
     *
     * <p>从 Spring Security 上下文获取当前登录用户 ID（由 JWT 认证过滤器写入），
     * 将新密码 BCrypt 加密后通过 RPC 调用用户服务更新。userId 由 RPC 拦截器透传。
     *
     * @param reqVO 修改密码请求入参
     * @return 操作结果
     */
    @Override
    public Response<?> updatePassword(UpdatePasswordReqVO reqVO) {
        // 从 JWT 上下文取当前登录用户 ID，并写入 RPC 透传上下文
        // （直连认证服务时无网关注入的 userId 头，需在此显式桥接，JWT 为准）
        Long userId = currentUserId();
        LoginUserContextHolder.setUserId(userId);
        try {
            String encodedPassword = passwordEncoder.encode(reqVO.getNewPassword());
            userRpcService.updatePassword(encodedPassword);
            return Response.success();
        } finally {
            LoginUserContextHolder.remove();
        }
    }

    /**
     * 退出登录.
     *
     * <p>将当前 JWT 写入 Redis 黑名单，TTL 对齐令牌剩余有效期；令牌已过期则无需处理。
     *
     * @param token 当前请求携带的 JWT
     * @return 操作结果
     */
    @Override
    public Response<?> logout(String token) {
        if (StringUtils.isBlank(token)) {
            return Response.success();
        }

        long ttlMillis = jwtTokenProvider.getExpiration(token).getTime() - System.currentTimeMillis();
        if (ttlMillis > 0) {
            String blacklistKey = RedisKeyConstants.buildTokenBlacklistKey(token);
            redisTemplate.opsForValue().set(blacklistKey, "1", ttlMillis, TimeUnit.MILLISECONDS);
        }

        log.info("==> 用户退出登录, userId: {}", currentUserId());
        return Response.success();
    }

    private Long currentUserId() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof HannoteUserDetails userDetails)) {
            throw new BizException(ResponseCodeEnum.UNAUTHORIZED);
        }
        return userDetails.getUserId();
    }

    /**
     * 从用户服务返回的角色列表中解析角色 key，缺失时回退默认普通用户角色.
     */
    private List<String> resolveRoleKeys(FindUserByPhoneRspDTO rsp) {
        List<String> roleKeys = rsp.getRoleKeys();
        if (Objects.isNull(roleKeys) || roleKeys.isEmpty()) {
            return List.of(AuthConstants.COMMON_USER_ROLE_KEY);
        }
        return roleKeys;
    }
}
