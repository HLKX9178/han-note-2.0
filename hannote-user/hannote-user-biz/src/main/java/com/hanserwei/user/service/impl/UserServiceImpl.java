package com.hanserwei.user.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.google.common.base.Preconditions;
import com.hanserwei.framework.biz.context.holder.LoginUserContextHolder;
import com.hanserwei.framework.common.enums.DeletedEnum;
import com.hanserwei.framework.common.enums.StatusEnum;
import com.hanserwei.framework.common.exception.BizException;
import com.hanserwei.framework.common.response.Response;
import com.hanserwei.framework.common.util.JsonUtils;
import com.hanserwei.framework.common.util.ParamUtils;
import com.hanserwei.user.api.dto.req.FindUserByPhoneReqDTO;
import com.hanserwei.user.api.dto.req.RegisterUserReqDTO;
import com.hanserwei.user.api.dto.req.UpdateUserPasswordReqDTO;
import com.hanserwei.user.api.dto.resp.FindUserByPhoneRspDTO;
import com.hanserwei.user.constant.RedisKeyConstants;
import com.hanserwei.user.constant.RoleConstants;
import com.hanserwei.user.domain.dataobject.RoleDO;
import com.hanserwei.user.domain.dataobject.UserDO;
import com.hanserwei.user.domain.dataobject.UserRoleDO;
import com.hanserwei.user.domain.mapper.RoleDOMapper;
import com.hanserwei.user.domain.mapper.UserDOMapper;
import com.hanserwei.user.domain.mapper.UserRoleDOMapper;
import com.hanserwei.user.enums.ResponseCodeEnum;
import com.hanserwei.user.enums.SexEnum;
import com.hanserwei.user.model.vo.UpdateUserInfoReqVO;
import com.hanserwei.user.rpc.DistributedIdRpcService;
import com.hanserwei.user.rpc.OssRpcService;
import com.hanserwei.user.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * 用户业务实现.
 *
 * <p>用户服务是 {@code t_user} 及角色/权限关系的唯一属主，提供：资料修改、
 * 注册（供认证服务 RPC 调用）、按手机号查询、密码更新。
 *
 * @author hanserwei
 * @date 2026/07/07
 * @since 0.0.1
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserDOMapper userDOMapper;
    private final UserRoleDOMapper userRoleDOMapper;
    private final RoleDOMapper roleDOMapper;
    private final OssRpcService ossRpcService;
    private final DistributedIdRpcService distributedIdRpcService;
    private final RedisTemplate<String, Object> redisTemplate;
    private final TransactionTemplate transactionTemplate;

    @Override
    public Response<?> updateUserInfo(UpdateUserInfoReqVO updateUserInfoReqVO) {
        UserDO userDO = new UserDO();
        userDO.setId(LoginUserContextHolder.getUserId());
        boolean needUpdate = false;

        // 头像
        MultipartFile avatarFile = updateUserInfoReqVO.getAvatar();
        if (Objects.nonNull(avatarFile)) {
            String avatar = ossRpcService.uploadFile(avatarFile);
            log.info("==> 调用 oss 上传头像，url: {}", avatar);
            if (StringUtils.isBlank(avatar)) {
                throw new BizException(ResponseCodeEnum.UPLOAD_AVATAR_FAIL);
            }
            userDO.setAvatar(avatar);
            needUpdate = true;
        }

        // 昵称
        String nickname = updateUserInfoReqVO.getNickname();
        if (StringUtils.isNotBlank(nickname)) {
            Preconditions.checkArgument(ParamUtils.checkNickname(nickname),
                    ResponseCodeEnum.NICK_NAME_VALID_FAIL.getErrorMessage());
            userDO.setNickname(nickname);
            needUpdate = true;
        }

        // hannote 号
        String hannoteId = updateUserInfoReqVO.getHannoteId();
        if (StringUtils.isNotBlank(hannoteId)) {
            Preconditions.checkArgument(ParamUtils.checkHannoteId(hannoteId),
                    ResponseCodeEnum.HANNOTE_ID_VALID_FAIL.getErrorMessage());
            userDO.setHannoteId(hannoteId);
            needUpdate = true;
        }

        // 性别
        Integer sex = updateUserInfoReqVO.getSex();
        if (Objects.nonNull(sex)) {
            Preconditions.checkArgument(SexEnum.isValid(sex),
                    ResponseCodeEnum.SEX_VALID_FAIL.getErrorMessage());
            userDO.setSex(sex);
            needUpdate = true;
        }

        // 生日
        LocalDate birthday = updateUserInfoReqVO.getBirthday();
        if (Objects.nonNull(birthday)) {
            userDO.setBirthday(birthday);
            needUpdate = true;
        }

        // 个人简介
        String introduction = updateUserInfoReqVO.getIntroduction();
        if (StringUtils.isNotBlank(introduction)) {
            Preconditions.checkArgument(ParamUtils.checkLength(introduction, 100),
                    ResponseCodeEnum.INTRODUCTION_VALID_FAIL.getErrorMessage());
            userDO.setIntroduction(introduction);
            needUpdate = true;
        }

        // 背景图
        MultipartFile backgroundImgFile = updateUserInfoReqVO.getBackgroundImg();
        if (Objects.nonNull(backgroundImgFile)) {
            String backgroundImg = ossRpcService.uploadFile(backgroundImgFile);
            log.info("==> 调用 oss 上传背景图，url: {}", backgroundImg);
            if (StringUtils.isBlank(backgroundImg)) {
                throw new BizException(ResponseCodeEnum.UPLOAD_BACKGROUND_IMG_FAIL);
            }
            userDO.setBackgroundImg(backgroundImg);
            needUpdate = true;
        }

        if (needUpdate) {
            userDO.setUpdateTime(LocalDateTime.now());
            userDOMapper.updateById(userDO);
        }
        return Response.success();
    }

    @Override
    public Response<Long> register(RegisterUserReqDTO registerUserReqDTO) {
        String phone = registerUserReqDTO.getPhone();

        // 幂等：手机号已注册则直接返回既有用户 ID
        UserDO existed = selectByPhone(phone);
        if (Objects.nonNull(existed)) {
            log.info("==> 手机号已注册, phone: {}, userId: {}", phone, existed.getId());
            return Response.success(existed.getId());
        }

        // 编程式事务保证：入库用户 + 分配角色 的原子性
        Long userId = transactionTemplate.execute(status -> {
            try {
                // 1. RPC: 调用分布式 ID 服务生成小憨书 ID
                Long hannoteId = distributedIdRpcService.generateHannoteId();
                Preconditions.checkNotNull(hannoteId, "小憨书 ID 生成失败");

                // 2. RPC: 调用分布式 ID 服务生成用户 ID
                Long newUserId = distributedIdRpcService.generateUserId();
                Preconditions.checkNotNull(newUserId, "用户 ID 生成失败");

                // 3. 入库用户（显式指定主键 ID）
                UserDO userDO = UserDO.builder()
                        .id(newUserId)
                        .phone(phone)
                        .hannoteId(String.valueOf(hannoteId))
                        .nickname("小憨薯" + hannoteId)
                        .status(StatusEnum.ENABLE.getValue())
                        .createTime(LocalDateTime.now())
                        .updateTime(LocalDateTime.now())
                        .deleted(DeletedEnum.NO.getValue())
                        .build();
                userDOMapper.insert(userDO);

                // 4. 分配普通用户角色
                UserRoleDO userRoleDO = UserRoleDO.builder()
                        .userId(newUserId)
                        .roleId(RoleConstants.COMMON_USER_ROLE_ID)
                        .createTime(LocalDateTime.now())
                        .updateTime(LocalDateTime.now())
                        .deleted(DeletedEnum.NO.getValue())
                        .build();
                userRoleDOMapper.insert(userRoleDO);

                return newUserId;
            } catch (Exception e) {
                status.setRollbackOnly();
                log.error("==> 系统注册用户异常: ", e);
                throw e;
            }
        });

        if (Objects.isNull(userId)) {
            throw new BizException(ResponseCodeEnum.REGISTER_FAIL);
        }

        // 4. 缓存用户角色到 Redis（供后续登录读取）
        List<String> roleKeys = new ArrayList<>(List.of(RoleConstants.COMMON_USER_ROLE_KEY));
        redisTemplate.opsForValue().set(RedisKeyConstants.buildUserRoleKey(phone), JsonUtils.toJsonString(roleKeys));

        return Response.success(userId);
    }

    @Override
    public Response<FindUserByPhoneRspDTO> findByPhone(FindUserByPhoneReqDTO findUserByPhoneReqDTO) {
        String phone = findUserByPhoneReqDTO.getPhone();

        UserDO userDO = selectByPhone(phone);
        if (Objects.isNull(userDO)) {
            throw new BizException(ResponseCodeEnum.USER_NOT_FOUND);
        }

        FindUserByPhoneRspDTO rspDTO = FindUserByPhoneRspDTO.builder()
                .id(userDO.getId())
                .password(userDO.getPassword())
                .roleKeys(loadRoleKeys(phone, userDO.getId()))
                .build();

        return Response.success(rspDTO);
    }

    @Override
    public Response<?> updatePassword(UpdateUserPasswordReqDTO updateUserPasswordReqDTO) {
        Long userId = LoginUserContextHolder.getUserId();
        if (Objects.isNull(userId)) {
            throw new BizException(ResponseCodeEnum.USER_NOT_FOUND);
        }

        UserDO userDO = UserDO.builder()
                .id(userId)
                .password(updateUserPasswordReqDTO.getEncodePassword())
                .updateTime(LocalDateTime.now())
                .build();
        userDOMapper.updateById(userDO);

        return Response.success();
    }

    /**
     * 根据手机号查询用户（取最新一条，避免历史脏数据导致 selectOne 抛错）.
     *
     * @param phone 手机号
     * @return 用户 DO；不存在返回 {@code null}
     */
    private UserDO selectByPhone(String phone) {
        List<UserDO> matched = userDOMapper.selectList(
                new LambdaQueryWrapper<UserDO>()
                        .eq(UserDO::getPhone, phone)
                        .orderByDesc(UserDO::getId)
                        .last("FETCH FIRST 1 ROWS ONLY")
        );
        return matched.isEmpty() ? null : matched.getFirst();
    }

    /**
     * 加载用户角色 key 列表：优先读 Redis 缓存，缺失则查库并回填缓存.
     *
     * @param phone  手机号（缓存 key）
     * @param userId 用户 ID（查库用）
     * @return 角色 key 列表；无角色时返回默认普通用户角色
     */
    @SuppressWarnings("unchecked")
    private List<String> loadRoleKeys(String phone, Long userId) {
        String cacheKey = RedisKeyConstants.buildUserRoleKey(phone);
        Object cached = redisTemplate.opsForValue().get(cacheKey);
        if (cached instanceof List<?> list && !list.isEmpty()) {
            return (List<String>) list;
        }

        // 缓存缺失：查 t_user_role_rel + t_role
        List<UserRoleDO> userRoles = userRoleDOMapper.selectList(
                new LambdaQueryWrapper<UserRoleDO>().eq(UserRoleDO::getUserId, userId));
        List<String> roleKeys;
        if (userRoles.isEmpty()) {
            roleKeys = new ArrayList<>(List.of(RoleConstants.COMMON_USER_ROLE_KEY));
        } else {
            List<Long> roleIds = userRoles.stream().map(UserRoleDO::getRoleId).toList();
            roleKeys = roleDOMapper.selectBatchIds(roleIds).stream()
                    .map(RoleDO::getRoleKey)
                    .filter(Objects::nonNull)
                    .collect(java.util.stream.Collectors.toCollection(ArrayList::new));
            if (roleKeys.isEmpty()) {
                roleKeys.add(RoleConstants.COMMON_USER_ROLE_KEY);
            }
        }

        // 回填缓存
        redisTemplate.opsForValue().set(cacheKey, JsonUtils.toJsonString(roleKeys));
        return roleKeys;
    }
}
