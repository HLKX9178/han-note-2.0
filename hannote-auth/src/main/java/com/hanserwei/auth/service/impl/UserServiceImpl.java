package com.hanserwei.auth.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.google.common.base.Preconditions;
import com.hanserwei.auth.constant.RedisKeyConstants;
import com.hanserwei.auth.constant.RoleConstants;
import com.hanserwei.auth.domain.dataobject.UserDO;
import com.hanserwei.auth.domain.dataobject.UserRoleDO;
import com.hanserwei.auth.domain.mapper.UserDOMapper;
import com.hanserwei.auth.domain.mapper.UserRoleDOMapper;
import com.hanserwei.auth.enums.LoginTypeEnum;
import com.hanserwei.auth.enums.ResponseCodeEnum;
import com.hanserwei.auth.model.vo.user.UserLoginReqVO;
import com.hanserwei.auth.security.JwtTokenProvider;
import com.hanserwei.auth.service.UserService;
import com.hanserwei.framework.common.enums.DeletedEnum;
import com.hanserwei.framework.common.enums.StatusEnum;
import com.hanserwei.framework.common.exception.BizException;
import com.hanserwei.framework.common.response.Response;
import com.hanserwei.framework.common.util.JsonUtils;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * 用户业务实现.
 *
 * <p>提供登录（新用户自动注册）能力，支持验证码 / 密码两种登录方式。
 * 注册流程使用编程式事务（{@link TransactionTemplate}），避免 {@code @Transactional} 自调用失效。
 *
 * @author hanserwei
 * @date 2026/07/07
 * @since 0.0.1
 */
@Service
@Slf4j
public class UserServiceImpl implements UserService {

    @Resource
    private UserDOMapper userDOMapper;

    @Resource
    private UserRoleDOMapper userRoleDOMapper;

    @Resource
    private RedisTemplate<String, Object> redisTemplate;

    @Resource
    private TransactionTemplate transactionTemplate;

    @Resource
    private JwtTokenProvider jwtTokenProvider;

    /**
     * 登录（新用户自动注册）.
     *
     * <p>流程：
     * <ol>
     *   <li>校验登录类型；</li>
     *   <li>验证码登录：校验验证码 → 未注册则自动注册 → 签发 JWT；</li>
     *   <li>密码登录：阶段一暂不支持。</li>
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

        // 1. 查询用户是否已注册（避免 selectOne 在数据异常时抛错，改用 selectList + getFirst）
        List<UserDO> matched = userDOMapper.selectList(
                new LambdaQueryWrapper<UserDO>()
                        .eq(UserDO::getPhone, phone)
                        .orderByDesc(UserDO::getId)
                        .last("FETCH FIRST 1 ROWS ONLY")
        );
        UserDO userDO = matched.isEmpty() ? null : matched.getFirst();

        Long userId;
        List<String> roleKeys;

        // 2. 按登录类型分派处理
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

                if (Objects.isNull(userDO)) {
                    // 新用户：自动注册
                    userId = registerUser(phone);
                    roleKeys = List.of(RoleConstants.COMMON_USER_ROLE_KEY);
                } else {
                    userId = userDO.getId();
                    roleKeys = loadRoleKeys(phone);
                }
                break;

            case PASSWORD:
                // 阶段一暂不支持密码登录
                throw new BizException(ResponseCodeEnum.LOGIN_TYPE_NOT_SUPPORT);

            default:
                throw new BizException(ResponseCodeEnum.LOGIN_TYPE_NOT_SUPPORT);
        }

        // 3. 签发 JWT
        String token = jwtTokenProvider.generateToken(userId, phone, roleKeys);
        return Response.success(token);
    }

    /**
     * 系统自动注册用户.
     *
     * <p>使用编程式事务保证以下步骤的原子性：
     * 生成全局小憨书 ID → 入库用户 → 分配普通用户角色 → 缓存用户角色到 Redis。
     *
     * @param phone 手机号
     * @return 注册成功后返回用户 ID
     */
    private Long registerUser(String phone) {
        return transactionTemplate.execute(status -> {
            try {
                // 1. 全局自增的小憨书 ID
                Long hannoteId = redisTemplate.opsForValue().increment(RedisKeyConstants.HANNOTE_ID_GENERATOR);
                Preconditions.checkNotNull(hannoteId, "小憨书 ID 生成失败");

                // 2. 入库用户
                UserDO userDO = UserDO.builder()
                        .phone(phone)
                        .hannoteId(String.valueOf(hannoteId))
                        .nickname("小憨薯" + hannoteId)
                        .status(StatusEnum.ENABLE.getValue())
                        .createTime(LocalDateTime.now())
                        .updateTime(LocalDateTime.now())
                        .deleted(DeletedEnum.NO.getValue())
                        .build();
                userDOMapper.insert(userDO);

                Long userId = userDO.getId();

                // 3. 分配普通用户角色
                UserRoleDO userRoleDO = UserRoleDO.builder()
                        .userId(userId)
                        .roleId(RoleConstants.COMMON_USER_ROLE_ID)
                        .createTime(LocalDateTime.now())
                        .updateTime(LocalDateTime.now())
                        .deleted(DeletedEnum.NO.getValue())
                        .build();
                userRoleDOMapper.insert(userRoleDO);

                // 4. 缓存用户角色到 Redis（供后续鉴权使用）
                List<String> roleKeys = new ArrayList<>(List.of(RoleConstants.COMMON_USER_ROLE_KEY));
                String userRolesKey = RedisKeyConstants.buildUserRoleKey(phone);
                redisTemplate.opsForValue().set(userRolesKey, JsonUtils.toJsonString(roleKeys));

                return userId;
            } catch (Exception e) {
                status.setRollbackOnly();
                log.error("==> 系统注册用户异常: ", e);
                throw e;
            }
        });
    }

    /**
     * 从 Redis 加载用户角色 key 列表.
     *
     * <p>兼容 {@code GenericJacksonJsonRedisSerializer} 自动反序列化为 {@link ArrayList} 的场景，
     * 同时兼容字符串形式（历史数据）。
     *
     * @param phone 手机号
     * @return 用户角色 key 列表；缓存缺失或解析失败时返回默认角色
     */
    @SuppressWarnings("unchecked")
    private List<String> loadRoleKeys(String phone) {
        String userRolesKey = RedisKeyConstants.buildUserRoleKey(phone);
        Object cached = redisTemplate.opsForValue().get(userRolesKey);
        if (cached == null) {
            return List.of(RoleConstants.COMMON_USER_ROLE_KEY);
        }
        if (cached instanceof List) {
            return (List<String>) cached;
        }
        try {
            List<?> parsed = JsonUtils.parseObject(String.valueOf(cached), List.class);
            return parsed == null ? List.of(RoleConstants.COMMON_USER_ROLE_KEY) : (List<String>) parsed;
        } catch (Exception e) {
            log.warn("解析用户角色缓存失败，使用默认角色", e);
            return List.of(RoleConstants.COMMON_USER_ROLE_KEY);
        }
    }
}
