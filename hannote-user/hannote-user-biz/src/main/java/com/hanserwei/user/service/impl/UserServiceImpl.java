package com.hanserwei.user.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.google.common.base.Preconditions;
import com.hanserwei.framework.biz.context.holder.LoginUserContextHolder;
import com.hanserwei.framework.common.enums.DeletedEnum;
import com.hanserwei.framework.common.enums.StatusEnum;
import com.hanserwei.framework.common.exception.BizException;
import com.hanserwei.framework.common.response.Response;
import com.hanserwei.count.api.dto.resp.FindUserCountRspDTO;
import com.hanserwei.framework.common.util.DateUtils;
import com.hanserwei.framework.common.util.JsonUtils;
import com.hanserwei.framework.common.util.NumberUtils;
import com.hanserwei.framework.common.util.ParamUtils;
import com.hanserwei.user.api.dto.req.FindUserByIdReqDTO;
import com.hanserwei.user.api.dto.req.FindUserByPhoneReqDTO;
import com.hanserwei.user.api.dto.req.FindUsersByIdsReqDTO;
import com.hanserwei.user.api.dto.req.RegisterUserReqDTO;
import com.hanserwei.user.api.dto.req.UpdateUserPasswordReqDTO;
import com.hanserwei.user.api.dto.resp.FindUserByIdRspDTO;
import com.hanserwei.user.api.dto.resp.FindUserByPhoneRspDTO;
import com.hanserwei.user.constant.MQConstants;
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
import com.hanserwei.user.model.vo.FindUserProfileReqVO;
import com.hanserwei.user.model.vo.FindUserProfileRspVO;
import com.hanserwei.user.model.vo.UpdateUserInfoReqVO;
import com.hanserwei.user.rpc.CountRpcService;
import com.hanserwei.user.rpc.DistributedIdRpcService;
import com.hanserwei.user.rpc.OssRpcService;
import com.hanserwei.user.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.rocketmq.client.producer.SendCallback;
import org.apache.rocketmq.client.producer.SendResult;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.jspecify.annotations.NonNull;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.data.redis.core.RedisOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.SessionCallback;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

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
    private final CountRpcService countRpcService;
    private final RedisTemplate<String, Object> redisTemplate;
    private final TransactionTemplate transactionTemplate;
    private final RocketMQTemplate rocketMQTemplate;
    /** 缓存回写执行器（虚拟线程，见 AsyncConfig） */
    private final ExecutorService cacheWriteExecutor;

    /** 空值哨兵：命中表示 DB 中确无此用户（防缓存穿透） */
    private static final String NULL_VALUE = "null";
    /** 用户信息本地缓存 L1（Caffeine） */
    private static final Cache<Long, FindUserByIdRspDTO> LOCAL_CACHE = Caffeine.newBuilder()
            .initialCapacity(10000)
            .maximumSize(10000)
            .expireAfterWrite(1, TimeUnit.HOURS)
            .build();
    /** 用户主页信息本地缓存 L1（Caffeine）：热点数据，5 分钟过期（短于 Redis，缩小不一致窗口） */
    private static final Cache<Long, FindUserProfileRspVO> PROFILE_LOCAL_CACHE = Caffeine.newBuilder()
            .initialCapacity(10000)
            .maximumSize(10000)
            .expireAfterWrite(5, TimeUnit.MINUTES)
            .build();

    @Override
    public Response<?> updateUserInfo(UpdateUserInfoReqVO updateUserInfoReqVO) {
        // 权限校验：仅号主本人可修改
        Long userId = updateUserInfoReqVO.getUserId();
        Long loginUserId = LoginUserContextHolder.getUserId();
        if (!Objects.equals(userId, loginUserId)) {
            throw new BizException(ResponseCodeEnum.CANT_UPDATE_OTHER_USER_PROFILE);
        }

        UserDO userDO = new UserDO();
        userDO.setId(userId);
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
            // 先删 Redis 缓存（用户信息 + 主页信息）
            deleteUserRedisCache(userId);

            userDO.setUpdateTime(LocalDateTime.now());
            userDOMapper.updateById(userDO);

            // 延迟双删：约 1s 后二次删除，兜底并发回填脏数据
            sendDelayDeleteUserRedisCacheMq(userId);

            // 通知搜索服务重建用户 ES 文档；昵称 / 头像变更时连带重建该用户全部笔记文档
            // （笔记索引冗余了 creator_nickname / creator_avatar）
            boolean creatorFieldChanged = Objects.nonNull(avatarFile) || StringUtils.isNotBlank(nickname);
            String tag = creatorFieldChanged
                    ? MQConstants.TAG_SYNC_ES_REBUILD_USER_AND_NOTES
                    : MQConstants.TAG_SYNC_ES_REBUILD_USER;
            sendUserSyncEsMq(userDO.getId(), tag);
        }
        return Response.success();
    }

    /**
     * 删除 Redis 中的用户信息缓存与主页信息缓存.
     *
     * @param userId 用户 ID
     */
    private void deleteUserRedisCache(Long userId) {
        redisTemplate.delete(java.util.Arrays.asList(
                RedisKeyConstants.buildUserInfoKey(userId),
                RedisKeyConstants.buildUserProfileKey(userId)));
    }

    /**
     * 异步发送延时消息，约 1s 后二次删除 Redis 用户缓存.
     *
     * @param userId 用户 ID
     */
    private void sendDelayDeleteUserRedisCacheMq(Long userId) {
        cacheWriteExecutor.execute(() -> {
            try {
                rocketMQTemplate.syncSendDelayTimeSeconds(
                        MQConstants.TOPIC_DELAY_DELETE_USER_REDIS_CACHE,
                        String.valueOf(userId), 1);
                log.info("==> MQ：延时删除 Redis 用户缓存消息发送成功, userId: {}", userId);
            } catch (Exception e) {
                log.error("==> MQ：延时删除 Redis 用户缓存消息发送失败, userId: {}", userId, e);
            }
        });
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

        // 5. 通知搜索服务重建用户 ES 文档（事务已提交，新用户暂无笔记，仅重建用户文档）
        sendUserSyncEsMq(userId, MQConstants.TAG_SYNC_ES_REBUILD_USER);

        return Response.success(userId);
    }

    /**
     * 发送用户 ES 同步消息（顺序发送，hashKey=userId）.
     *
     * @param userId 用户 ID
     * @param tag    {@link MQConstants#TAG_SYNC_ES_REBUILD_USER} 或
     *               {@link MQConstants#TAG_SYNC_ES_REBUILD_USER_AND_NOTES}
     */
    private void sendUserSyncEsMq(Long userId, String tag) {
        Message<String> message = MessageBuilder.withPayload(String.valueOf(userId)).build();
        String destination = MQConstants.TOPIC_USER_SYNC_ES + ":" + tag;

        rocketMQTemplate.asyncSendOrderly(destination, message, String.valueOf(userId), new SendCallback() {
            @Override
            public void onSuccess(SendResult sendResult) {
                log.info("==> 【用户：ES 同步-{}】MQ 发送成功, userId: {}", tag, userId);
            }

            @Override
            public void onException(Throwable throwable) {
                log.error("==> 【用户：ES 同步-{}】MQ 发送异常, userId: {}", tag, userId, throwable);
            }
        });
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

    @Override
    public Response<FindUserByIdRspDTO> findById(FindUserByIdReqDTO findUserByIdReqDTO) {
        Long userId = findUserByIdReqDTO.getId();
        String redisKey = RedisKeyConstants.buildUserInfoKey(userId);

        // 1. L1 本地缓存（Caffeine）
        FindUserByIdRspDTO localCached = LOCAL_CACHE.getIfPresent(userId);
        if (Objects.nonNull(localCached)) {
            log.info("==> 命中本地缓存, userId: {}", userId);
            return Response.success(localCached);
        }

        // 2. L2 分布式缓存（Redis）
        Object redisValue = redisTemplate.opsForValue().get(redisKey);
        if (Objects.nonNull(redisValue)) {
            // 命中空值哨兵：DB 确无此用户，直接抛异常（防穿透）
            if (NULL_VALUE.equals(redisValue)) {
                throw new BizException(ResponseCodeEnum.USER_NOT_FOUND);
            }
            FindUserByIdRspDTO rspDTO = JsonUtils.parseObject(String.valueOf(redisValue), FindUserByIdRspDTO.class);
            // 异步回填 L1
            cacheWriteExecutor.execute(() -> LOCAL_CACHE.put(userId, rspDTO));
            return Response.success(rspDTO);
        }

        // 3. 回源数据库
        UserDO userDO = userDOMapper.selectById(userId);

        // 3.1 用户不存在：异步写空值短 TTL（防穿透），抛异常
        if (Objects.isNull(userDO)) {
            cacheWriteExecutor.execute(() -> {
                long expire = 60 + ThreadLocalRandom.current().nextInt(60);
                redisTemplate.opsForValue().set(redisKey, NULL_VALUE, Duration.ofSeconds(expire));
            });
            throw new BizException(ResponseCodeEnum.USER_NOT_FOUND);
        }

        // 3.2 命中：构建返参，异步写 L1 + L2（长 TTL + 随机秒，防雪崩）
        FindUserByIdRspDTO rspDTO = FindUserByIdRspDTO.builder()
                .id(userDO.getId())
                .nickName(userDO.getNickname())
                .avatar(userDO.getAvatar())
                .introduction(userDO.getIntroduction())
                .build();
        cacheWriteExecutor.execute(() -> {
            LOCAL_CACHE.put(userId, rspDTO);
            long expire = Duration.ofDays(1).toSeconds() + ThreadLocalRandom.current().nextInt(60 * 60 * 4);
            redisTemplate.opsForValue().set(redisKey, JsonUtils.toJsonString(rspDTO), Duration.ofSeconds(expire));
        });

        return Response.success(rspDTO);
    }

    @Override
    public Response<List<FindUserByIdRspDTO>> findByIds(FindUsersByIdsReqDTO findUsersByIdsReqDTO) {
        List<Long> userIds = findUsersByIdsReqDTO.getIds();

        // 1. 批量查 L2 Redis（multiGet 一次往返），按位置对应回 userId
        List<String> redisKeys = userIds.stream()
                .map(RedisKeyConstants::buildUserInfoKey)
                .toList();
        List<Object> redisValues = redisTemplate.opsForValue().multiGet(redisKeys);

        // userId -> 用户信息，收集所有命中/回源结果，最终按入参 userIds 顺序输出
        Map<Long, FindUserByIdRspDTO> userMap = new HashMap<>();
        // 待回源 DB 的 userId 列表
        List<Long> userIdsNeedQuery = new ArrayList<>();

        for (int i = 0; i < userIds.size(); i++) {
            Long userId = userIds.get(i);
            Object value = Objects.isNull(redisValues) ? null : redisValues.get(i);
            if (Objects.isNull(value)) {
                // 真未命中：回源 DB
                userIdsNeedQuery.add(userId);
            } else if (NULL_VALUE.equals(value)) {
                // 命中空值哨兵：DB 确无此用户，既不进结果也不回源（防穿透）
                log.info("==> 命中空值哨兵, userId: {}", userId);
            } else {
                // 命中真实缓存
                FindUserByIdRspDTO dto = JsonUtils.parseObject(String.valueOf(value), FindUserByIdRspDTO.class);
                userMap.put(userId, dto);
            }
        }

        // 2. 存在需回源的 userId：查库 + 回写缓存
        if (!userIdsNeedQuery.isEmpty()) {
            // 回源数据库（selectByIds 自动带 is_deleted=false 过滤）
            List<UserDO> userDOS = userDOMapper.selectByIds(userIdsNeedQuery);
            List<FindUserByIdRspDTO> dbResult = Objects.isNull(userDOS) ? List.of() : userDOS.stream()
                    .map(userDO -> FindUserByIdRspDTO.builder()
                            .id(userDO.getId())
                            .nickName(userDO.getNickname())
                            .avatar(userDO.getAvatar())
                            .introduction(userDO.getIntroduction())
                            .build())
                    .toList();
            dbResult.forEach(dto -> userMap.put(dto.getId(), dto));

            // 异步 pipeline 回写 L2 Redis（长 TTL + 随机秒防雪崩）
            if (!dbResult.isEmpty()) {
                syncUsersToRedis(dbResult);
            }

            // DB 亦查无的 userId：异步写空值哨兵（短 TTL），防批量查询反复穿透
            List<Long> notExistedIds = userIdsNeedQuery.stream()
                    .filter(id -> !userMap.containsKey(id))
                    .toList();
            if (!notExistedIds.isEmpty()) {
                syncNullValueToRedis(notExistedIds);
            }
        }

        // 3. 按入参 userIds 顺序输出（保证调用方拿到的顺序与请求一致，如关注列表按关注时间倒序）
        List<FindUserByIdRspDTO> result = userIds.stream()
                .map(userMap::get)
                .filter(Objects::nonNull)
                .toList();

        return Response.success(result);
    }

    /**
     * 异步批量写入空值哨兵（短 TTL），防止对不存在用户的批量查询反复穿透到 DB.
     *
     * @param notExistedIds DB 中确不存在的 userId 列表
     */
    private void syncNullValueToRedis(List<Long> notExistedIds) {
        cacheWriteExecutor.execute(() -> redisTemplate.executePipelined(new SessionCallback<Object>() {
            @Override
            @SuppressWarnings({"unchecked", "rawtypes"})
            public Object execute(RedisOperations operations) {
                notExistedIds.forEach(userId -> {
                    String key = RedisKeyConstants.buildUserInfoKey(userId);
                    long expire = 60 + ThreadLocalRandom.current().nextInt(60);
                    operations.opsForValue().set(key, NULL_VALUE, Duration.ofSeconds(expire));
                });
                return null;
            }
        }));
    }

    /**
     * 异步线程 + pipeline 管道，将回源查询到的用户信息批量同步到 Redis（减少网络往返）.
     *
     * @param users 需回写的用户信息列表
     */
    private void syncUsersToRedis(List<FindUserByIdRspDTO> users) {
        cacheWriteExecutor.execute(() -> {
            Map<Long, FindUserByIdRspDTO> map = users.stream()
                    .collect(Collectors.toMap(FindUserByIdRspDTO::getId, u -> u));
            redisTemplate.executePipelined(new SessionCallback<Object>() {
                @Override
                @SuppressWarnings({"unchecked"})
                public Object execute(@NonNull RedisOperations operations) {
                    map.forEach((userId, dto) -> {
                        String key = RedisKeyConstants.buildUserInfoKey(userId);
                        long expire = Duration.ofDays(1).toSeconds() + ThreadLocalRandom.current().nextInt(60 * 60 * 4);
                        operations.opsForValue().set(key, JsonUtils.toJsonString(dto), Duration.ofSeconds(expire));
                    });
                    return null;
                }
            });
        });
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
            roleKeys = roleDOMapper.selectByIds(roleIds).stream()
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

    @Override
    public Response<FindUserProfileRspVO> findUserProfile(FindUserProfileReqVO findUserProfileReqVO) {
        Long userId = findUserProfileReqVO.getUserId();
        if (Objects.isNull(userId)) {
            userId = LoginUserContextHolder.getUserId();
        }

        // 1. 非本人才查本地缓存；本人查看跳过本地缓存，保证改资料后自己立即可见
        if (!Objects.equals(userId, LoginUserContextHolder.getUserId())) {
            FindUserProfileRspVO localCache = PROFILE_LOCAL_CACHE.getIfPresent(userId);
            if (Objects.nonNull(localCache)) {
                log.info("==> 用户主页信息命中本地缓存, userId: {}", userId);
                return Response.success(localCache);
            }
        }

        // 2. 再查 Redis 缓存
        String profileRedisKey = RedisKeyConstants.buildUserProfileKey(userId);
        Object profileJson = redisTemplate.opsForValue().get(profileRedisKey);
        if (Objects.nonNull(profileJson)) {
            FindUserProfileRspVO cached = JsonUtils.parseObject(String.valueOf(profileJson), FindUserProfileRspVO.class);
            syncUserProfile2LocalCache(userId, cached);
            return Response.success(cached);
        }

        // 3. 回源数据库
        UserDO userDO = userDOMapper.selectById(userId);
        if (Objects.isNull(userDO)) {
            throw new BizException(ResponseCodeEnum.USER_NOT_FOUND);
        }
        FindUserProfileRspVO vo = FindUserProfileRspVO.builder()
                .userId(userDO.getId())
                .avatar(userDO.getAvatar())
                .nickname(userDO.getNickname())
                .hannoteId(userDO.getHannoteId())
                .sex(userDO.getSex())
                .introduction(userDO.getIntroduction())
                .age(Objects.isNull(userDO.getBirthday()) ? 0 : DateUtils.calculateAge(userDO.getBirthday()))
                .build();

        // 4. RPC 聚合计数
        rpcCountServiceAndSetData(userId, vo);

        // 5. 异步回写两级缓存
        syncUserProfile2Redis(profileRedisKey, vo);
        syncUserProfile2LocalCache(userId, vo);

        return Response.success(vo);
    }

    /**
     * 异步回写用户主页信息到 Redis（随机 TTL，2 小时内）.
     *
     * @param profileRedisKey Redis Key
     * @param vo              主页信息
     */
    private void syncUserProfile2Redis(String profileRedisKey, FindUserProfileRspVO vo) {
        cacheWriteExecutor.execute(() -> {
            long expireSeconds = 60L * 60 + ThreadLocalRandom.current().nextInt(60 * 60);
            redisTemplate.opsForValue().set(profileRedisKey, JsonUtils.toJsonString(vo), expireSeconds, TimeUnit.SECONDS);
        });
    }

    /**
     * 异步回写用户主页信息到本地缓存.
     *
     * @param userId 用户 ID
     * @param vo     主页信息
     */
    private void syncUserProfile2LocalCache(Long userId, FindUserProfileRspVO vo) {
        cacheWriteExecutor.execute(() -> PROFILE_LOCAL_CACHE.put(userId, vo));
    }

    /**
     * RPC 调用计数服务，将用户维度计数格式化后设置到主页 VO.
     *
     * @param userId 用户 ID
     * @param vo     主页信息返参（原地填充计数字段）
     */
    private void rpcCountServiceAndSetData(Long userId, FindUserProfileRspVO vo) {
        FindUserCountRspDTO count = countRpcService.findUserCountById(userId);
        if (Objects.nonNull(count)) {
            long likeTotal = Objects.isNull(count.getLikeTotal()) ? 0 : count.getLikeTotal();
            long collectTotal = Objects.isNull(count.getCollectTotal()) ? 0 : count.getCollectTotal();
            vo.setFansTotal(NumberUtils.formatNumberString(Objects.isNull(count.getFansTotal()) ? 0 : count.getFansTotal()));
            vo.setFollowingTotal(NumberUtils.formatNumberString(Objects.isNull(count.getFollowingTotal()) ? 0 : count.getFollowingTotal()));
            vo.setNoteTotal(NumberUtils.formatNumberString(Objects.isNull(count.getNoteTotal()) ? 0 : count.getNoteTotal()));
            vo.setLikeTotal(NumberUtils.formatNumberString(likeTotal));
            vo.setCollectTotal(NumberUtils.formatNumberString(collectTotal));
            vo.setLikeAndCollectTotal(NumberUtils.formatNumberString(likeTotal + collectTotal));
        }
    }
}
