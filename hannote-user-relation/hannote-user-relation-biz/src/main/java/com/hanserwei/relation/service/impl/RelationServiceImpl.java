package com.hanserwei.relation.service.impl;

import com.hanserwei.framework.biz.context.holder.LoginUserContextHolder;
import com.hanserwei.framework.common.exception.BizException;
import com.hanserwei.framework.common.response.PageResponse;
import com.hanserwei.framework.common.response.Response;
import com.hanserwei.framework.common.util.DateUtils;
import com.hanserwei.framework.common.util.JsonUtils;
import com.hanserwei.relation.config.RelationProperties;
import com.hanserwei.relation.constant.MQConstants;
import com.hanserwei.relation.model.dto.FollowUserMqDTO;
import com.hanserwei.relation.model.dto.UnfollowUserMqDTO;
import com.hanserwei.relation.constant.RedisKeyConstants;
import com.hanserwei.relation.domain.dataobject.FansDO;
import com.hanserwei.relation.domain.dataobject.FollowingDO;
import com.hanserwei.relation.domain.mapper.FansDOMapper;
import com.hanserwei.relation.domain.mapper.FollowingDOMapper;
import com.hanserwei.relation.enums.LuaResultEnum;
import com.hanserwei.relation.enums.ResponseCodeEnum;
import com.hanserwei.relation.model.vo.FindFansListReqVO;
import com.hanserwei.relation.model.vo.FindFansUserRspVO;
import com.hanserwei.relation.model.vo.FindFollowingListReqVO;
import com.hanserwei.relation.model.vo.FindFollowingUserRspVO;
import com.hanserwei.relation.model.vo.FollowUserReqVO;
import com.hanserwei.relation.model.vo.UnfollowUserReqVO;
import com.hanserwei.relation.rpc.UserRpcService;
import com.hanserwei.relation.service.RelationService;
import com.hanserwei.user.api.dto.resp.FindUserByIdRspDTO;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.client.producer.SendResult;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.scripting.support.ResourceScriptSource;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 用户关系业务实现.
 *
 * <p>关注接口采用「Redis ZSET 写缓冲 + Lua 原子写」方案：校验不能关注自己、RPC 校验目标用户存在后，
 * 通过 Lua 脚本原子完成「关注上限校验 + 重复关注校验 + ZADD」。ZSET 缓存缺失时从数据库回源同步。
 *
 * <p>注意：本期（��仅写入 Redis，异步落库（MQ 消费者写 t_following/t_fans）
 * 留待后续章节，见方法末尾 {@code // TODO: 发送 MQ}。
 *
 * @author hanserwei
 * @date 2026/07/10
 * @since 0.0.1
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RelationServiceImpl implements RelationService {

    private final StringRedisTemplate stringRedisTemplate;
    private final UserRpcService userRpcService;
    private final FollowingDOMapper followingDOMapper;
    private final FansDOMapper fansDOMapper;
    private final RelationProperties relationProperties;
    private final RocketMQTemplate rocketMQTemplate;
    /** 虚拟线程执行器，用于缓存缺失回源后异步全量回填 ZSET（Bean 名 relationTaskExecutor） */
    private final ExecutorService relationTaskExecutor;

    /** 关注列表 ZSET 保底过期时间：1 天（秒） */
    private static final long FOLLOWING_EXPIRE_BASE_SECONDS = 60 * 60 * 24;

    /** 列表接口每页数据量 */
    private static final long PAGE_SIZE = 10L;

    /** 粉丝列表数据库分页最大可查页数（超过直接返回空，防止深翻页） */
    private static final long FANS_MAX_PAGE = 500L;

    @Override
    public Response<?> follow(FollowUserReqVO followUserReqVO) {
        // 被关注的用户 ID
        Long followUserId = followUserReqVO.getFollowUserId();
        // 当前登录用户 ID
        Long userId = LoginUserContextHolder.getUserId();

        // 1. 校验：无法关注自己
        if (Objects.equals(userId, followUserId)) {
            throw new BizException(ResponseCodeEnum.CANT_FOLLOW_YOUR_SELF);
        }

        // 2. RPC 校验：被关注的用户是否真实存在
        FindUserByIdRspDTO followUser = userRpcService.findById(followUserId);
        if (Objects.isNull(followUser)) {
            throw new BizException(ResponseCodeEnum.FOLLOW_USER_NOT_EXISTED);
        }

        // 3. Lua 原子写入 Redis ZSET 关注列表
        String followingKey = RedisKeyConstants.buildUserFollowingKey(userId);
        // 关注时刻：ZSET score 与 MQ 落库 create_time 取同一值，保证缓存与库一致
        LocalDateTime now = LocalDateTime.now();
        long timestamp = DateUtils.localDateTime2Timestamp(now);
        int maxLimit = relationProperties.getFollowing().getMaxLimit();

        Long result = stringRedisTemplate.execute(CHECK_AND_ADD_SCRIPT,
                Collections.singletonList(followingKey),
                String.valueOf(followUserId), String.valueOf(timestamp), String.valueOf(maxLimit));

        // 校验脚本结果（达上限 / 已关注 直接抛异常）
        checkLuaScriptResult(result);

        // 4. ZSET 不存在：从数据库回源同步后再写入
        if (Objects.equals(result, LuaResultEnum.ZSET_NOT_EXIST.getCode())) {
            boolean dbEmpty = loadFollowingIntoRedis(userId, followingKey);
            if (!dbEmpty) {
                // 回源后重新执行校验并写入本次关注
                Long retry = stringRedisTemplate.execute(CHECK_AND_ADD_SCRIPT,
                        Collections.singletonList(followingKey),
                        String.valueOf(followUserId), String.valueOf(timestamp), String.valueOf(maxLimit));
                checkLuaScriptResult(retry);
            } else {
                // DB 亦无记录：直接写入首条关注关系并设置过期时间
                long expireSeconds = randomFollowingExpireSeconds();
                stringRedisTemplate.execute(ADD_AND_EXPIRE_SCRIPT,
                        Collections.singletonList(followingKey),
                        String.valueOf(followUserId), String.valueOf(timestamp), String.valueOf(expireSeconds));
            }
        }

        // 5. 发送 MQ，由消费者异步落库 t_following + t_fans（削峰、提升接口响应）
        sendFollowMq(userId, followUserId, now);

        return Response.success();
    }

    @Override
    public Response<?> unfollow(UnfollowUserReqVO unfollowUserReqVO) {
        // 被取关的用户 ID
        Long unfollowUserId = unfollowUserReqVO.getUnfollowUserId();
        // 当前登录用户 ID
        Long userId = LoginUserContextHolder.getUserId();

        // 1. 校验：无法取关自己
        if (Objects.equals(userId, unfollowUserId)) {
            throw new BizException(ResponseCodeEnum.CANT_UNFOLLOW_YOUR_SELF);
        }

        // 2. RPC 校验：被取关的用户是否真实存在
        FindUserByIdRspDTO unfollowUser = userRpcService.findById(unfollowUserId);
        if (Objects.isNull(unfollowUser)) {
            throw new BizException(ResponseCodeEnum.FOLLOW_USER_NOT_EXISTED);
        }

        // 3. Lua 原子校验并移除 Redis ZSET 中的关注关系
        String followingKey = RedisKeyConstants.buildUserFollowingKey(userId);
        Long result = stringRedisTemplate.execute(UNFOLLOW_CHECK_AND_DELETE_SCRIPT,
                Collections.singletonList(followingKey), String.valueOf(unfollowUserId));

        // 未关注对方，无法取关
        if (Objects.equals(result, LuaResultEnum.NOT_FOLLOWED.getCode())) {
            throw new BizException(ResponseCodeEnum.NOT_FOLLOWED);
        }

        // 4. ZSET 不存在（可能已过期）：从数据库回源同步后再次尝试移除
        if (Objects.equals(result, LuaResultEnum.ZSET_NOT_EXIST.getCode())) {
            boolean dbEmpty = loadFollowingIntoRedis(userId, followingKey);
            // DB 无任何关注记录：说明未关注任何人，无法取关
            if (dbEmpty) {
                throw new BizException(ResponseCodeEnum.NOT_FOLLOWED);
            }
            // 回源后重新执行校验并移除
            Long retry = stringRedisTemplate.execute(UNFOLLOW_CHECK_AND_DELETE_SCRIPT,
                    Collections.singletonList(followingKey), String.valueOf(unfollowUserId));
            if (Objects.equals(retry, LuaResultEnum.NOT_FOLLOWED.getCode())) {
                throw new BizException(ResponseCodeEnum.NOT_FOLLOWED);
            }
        }

        // 5. 发送 MQ，由消费者异步删除 t_following + t_fans、更新粉丝 ZSET
        sendUnfollowMq(userId, unfollowUserId, LocalDateTime.now());

        return Response.success();
    }

    @Override
    public PageResponse<FindFollowingUserRspVO> findFollowingList(FindFollowingListReqVO findFollowingListReqVO) {
        // 想要查询的用户 ID
        Long userId = findFollowingListReqVO.getUserId();
        // 页码
        Integer pageNo = findFollowingListReqVO.getPageNo();

        // 关注列表 ZSET Key
        String followingKey = RedisKeyConstants.buildUserFollowingKey(userId);
        // ZSET 总大小
        Long total = stringRedisTemplate.opsForZSet().zCard(followingKey);

        List<FindFollowingUserRspVO> rspVOS = null;

        if (Objects.nonNull(total) && total > 0) {
            // 缓存命中：直接从 ZSET 分页
            long totalPage = PageResponse.getTotalPage(total, PAGE_SIZE);
            // 请求页码超出总页数，返回空数据
            if (pageNo > totalPage) {
                return PageResponse.success(null, pageNo, total);
            }

            long offset = PageResponse.getOffset(pageNo, PAGE_SIZE);
            // 按 score（关注时间戳）降序分页取用户 ID
            Set<String> followingUserIds = stringRedisTemplate.opsForZSet()
                    .reverseRangeByScore(followingKey, Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY, offset, PAGE_SIZE);

            if (followingUserIds != null && !followingUserIds.isEmpty()) {
                List<Long> userIds = followingUserIds.stream().map(Long::valueOf).toList();
                rspVOS = rpcUserServiceAndDTO2VO(userIds);
            }
            return PageResponse.success(rspVOS, pageNo, total);
        }

        // 缓存缺失：从数据库分页查询（分页插件生成 PG 方言 SQL）
        IPage<FollowingDO> page = followingDOMapper.selectPage(
                new Page<>(pageNo, PAGE_SIZE),
                new LambdaQueryWrapper<FollowingDO>()
                        .eq(FollowingDO::getUserId, userId)
                        .orderByDesc(FollowingDO::getCreateTime));

        long count = page.getTotal();
        List<FollowingDO> records = page.getRecords();

        if (records != null && !records.isEmpty()) {
            List<Long> userIds = records.stream().map(FollowingDO::getFollowingUserId).toList();
            rspVOS = rpcUserServiceAndDTO2VO(userIds);
            // 异步全量同步关注列表至 Redis（下次查询直接命中缓存）
            relationTaskExecutor.submit(() -> syncFollowingList2Redis(userId));
        }

        return PageResponse.success(rspVOS, pageNo, count);
    }

    @Override
    public PageResponse<FindFansUserRspVO> findFansList(FindFansListReqVO findFansListReqVO) {
        // 想要查询的用户 ID
        Long userId = findFansListReqVO.getUserId();
        // 页码
        Integer pageNo = findFansListReqVO.getPageNo();

        // 粉丝列表 ZSET Key
        String fansKey = RedisKeyConstants.buildUserFansKey(userId);
        // ZSET 总大小
        Long total = stringRedisTemplate.opsForZSet().zCard(fansKey);

        List<FindFansUserRspVO> rspVOS = null;

        if (Objects.nonNull(total) && total > 0) {
            // 缓存命中：直接从 ZSET 分页
            long totalPage = PageResponse.getTotalPage(total, PAGE_SIZE);
            if (pageNo > totalPage) {
                return PageResponse.success(null, pageNo, total);
            }

            long offset = PageResponse.getOffset(pageNo, PAGE_SIZE);
            Set<String> fansUserIds = stringRedisTemplate.opsForZSet()
                    .reverseRangeByScore(fansKey, Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY, offset, PAGE_SIZE);

            if (fansUserIds != null && !fansUserIds.isEmpty()) {
                List<Long> userIds = fansUserIds.stream().map(Long::valueOf).toList();
                rspVOS = rpcUserServiceAndCountServiceAndDTO2VO(userIds);
            }
            return PageResponse.success(rspVOS, pageNo, total);
        }

        // 缓存缺失：粉丝列表数据库分页仅允许查询前 500 页
        if (pageNo > FANS_MAX_PAGE) {
            return PageResponse.success(null, pageNo, 0);
        }

        // 从数据库分页查询
        IPage<FansDO> page = fansDOMapper.selectPage(
                new Page<>(pageNo, PAGE_SIZE),
                new LambdaQueryWrapper<FansDO>()
                        .eq(FansDO::getUserId, userId)
                        .orderByDesc(FansDO::getCreateTime));

        long count = page.getTotal();
        List<FansDO> records = page.getRecords();

        if (records != null && !records.isEmpty()) {
            List<Long> userIds = records.stream().map(FansDO::getFansUserId).toList();
            rspVOS = rpcUserServiceAndCountServiceAndDTO2VO(userIds);
            // 异步同步粉丝列表至 Redis（最多 5000 条）
            relationTaskExecutor.submit(() -> syncFansList2Redis(userId));
        }

        return PageResponse.success(rspVOS, pageNo, count);
    }

    /**
     * RPC 批量查询用户信息并转换为关注列表 VO.
     *
     * @param userIds 用户 ID 列表（当前页，≤ 10）
     * @return 关注用户 VO 列表；无数据返回 {@code null}
     */
    private List<FindFollowingUserRspVO> rpcUserServiceAndDTO2VO(List<Long> userIds) {
        List<FindUserByIdRspDTO> userDTOS = userRpcService.findByIds(userIds);
        if (userDTOS == null || userDTOS.isEmpty()) {
            return null;
        }
        return userDTOS.stream()
                .map(dto -> FindFollowingUserRspVO.builder()
                        .userId(dto.getId())
                        .avatar(dto.getAvatar())
                        .nickname(dto.getNickName())
                        .introduction(dto.getIntroduction())
                        .build())
                .toList();
    }

    /**
     * RPC 批量查询用户信息（及计数）并转换为粉丝列表 VO.
     *
     * <p>计数服务尚未建设，{@code fansTotal} / {@code noteTotal} 暂固定为 0。
     *
     * @param userIds 用户 ID 列表（当前页，≤ 10）
     * @return 粉丝用户 VO 列表；无数据返回 {@code null}
     */
    private List<FindFansUserRspVO> rpcUserServiceAndCountServiceAndDTO2VO(List<Long> userIds) {
        List<FindUserByIdRspDTO> userDTOS = userRpcService.findByIds(userIds);
        // TODO: RPC 批量查询用户计数数据（笔记总数、粉丝总数），计数服务建设后补充
        if (userDTOS == null || userDTOS.isEmpty()) {
            return null;
        }
        return userDTOS.stream()
                .map(dto -> FindFansUserRspVO.builder()
                        .userId(dto.getId())
                        .avatar(dto.getAvatar())
                        .nickname(dto.getNickName())
                        .fansTotal(0L) // TODO: 计数服务建设后补充
                        .noteTotal(0L) // TODO: 计数服务建设后补充
                        .build())
                .toList();
    }

    /**
     * 全量同步关注列表至 Redis（最多 1000 条）.
     *
     * @param userId 目标用户 ID
     */
    private void syncFollowingList2Redis(Long userId) {
        // 查询全量关注关系（上限 1000，与关注上限一致）
        List<FollowingDO> followingDOS = followingDOMapper.selectList(
                new LambdaQueryWrapper<FollowingDO>()
                        .eq(FollowingDO::getUserId, userId)
                        .orderByDesc(FollowingDO::getCreateTime)
                        .last("LIMIT " + relationProperties.getFollowing().getMaxLimit()));

        if (followingDOS == null || followingDOS.isEmpty()) {
            return;
        }

        String followingKey = RedisKeyConstants.buildUserFollowingKey(userId);
        // 复用批量回填脚本参数构建（关注列表：score=关注时间戳，member=关注的用户 ID）
        Object[] batchArgs = buildBatchLuaArgs(followingDOS, randomFollowingExpireSeconds());
        stringRedisTemplate.execute(BATCH_ADD_AND_EXPIRE_SCRIPT,
                Collections.singletonList(followingKey), batchArgs);
    }

    /**
     * 全量同步粉丝列表至 Redis（最多 5000 条）.
     *
     * @param userId 目标用户 ID
     */
    private void syncFansList2Redis(Long userId) {
        // 查询最新的粉丝列表（上限 5000）
        List<FansDO> fansDOS = fansDOMapper.selectList(
                new LambdaQueryWrapper<FansDO>()
                        .eq(FansDO::getUserId, userId)
                        .orderByDesc(FansDO::getCreateTime)
                        .last("LIMIT " + relationProperties.getFans().getMaxCacheCount()));

        if (fansDOS == null || fansDOS.isEmpty()) {
            return;
        }

        String fansKey = RedisKeyConstants.buildUserFansKey(userId);
        // 复用批量回填脚本（粉丝列表：score=关注时间戳，member=粉丝的用户 ID）
        Object[] batchArgs = buildFansZSetLuaArgs(fansDOS, randomFollowingExpireSeconds());
        stringRedisTemplate.execute(BATCH_ADD_AND_EXPIRE_SCRIPT,
                Collections.singletonList(fansKey), batchArgs);
    }

    /**
     * 异步发送关注操作 MQ（携带 Follow 标签）。
     *
     * @param userId       发起关注的用户 ID
     * @param followUserId 被关注的用户 ID
     * @param createTime   关注时刻（与 ZSET score 一致）
     */
    private void sendFollowMq(Long userId, Long followUserId, LocalDateTime createTime) {
        FollowUserMqDTO followUserMqDTO = FollowUserMqDTO.builder()
                .userId(userId)
                .followUserId(followUserId)
                .createTime(createTime)
                .build();

        Message<String> message = MessageBuilder
                .withPayload(JsonUtils.toJsonString(followUserMqDTO))
                .build();

        // Topic:Tag —— 冒号连接使消息携带 Follow 标签
        String destination = MQConstants.TOPIC_FOLLOW_OR_UNFOLLOW + ":" + MQConstants.TAG_FOLLOW;
        log.info("==> 开始发送关注操作 MQ, 消息体: {}", followUserMqDTO);

        // 顺序发送：以发起者 userId 为 hashKey，保证同一用户的关注/取关消息进同一队列、按序消费
        SendResult sendResult = rocketMQTemplate.syncSendOrderly(destination, message, String.valueOf(userId));
        log.info("==> 关注操作 MQ 发送成功, SendResult: {}", sendResult);
    }

    /**
     * 顺序发送取关操作 MQ（携带 Unfollow 标签，hashKey = 发起者 userId）。
     *
     * @param userId         发起取关的用户 ID
     * @param unfollowUserId 被取关的用户 ID
     * @param createTime     取关时刻
     */
    private void sendUnfollowMq(Long userId, Long unfollowUserId, LocalDateTime createTime) {
        UnfollowUserMqDTO unfollowUserMqDTO = UnfollowUserMqDTO.builder()
                .userId(userId)
                .unfollowUserId(unfollowUserId)
                .createTime(createTime)
                .build();

        Message<String> message = MessageBuilder
                .withPayload(JsonUtils.toJsonString(unfollowUserMqDTO))
                .build();

        // Topic:Tag —— 冒号连接使消息携带 Unfollow 标签
        String destination = MQConstants.TOPIC_FOLLOW_OR_UNFOLLOW + ":" + MQConstants.TAG_UNFOLLOW;
        log.info("==> 开始发送取关操作 MQ, 消息体: {}", unfollowUserMqDTO);

        // 顺序发送：与关注共用 hashKey（发起者 userId），保证「关注→取关→关注」按序落库
        SendResult sendResult = rocketMQTemplate.syncSendOrderly(destination, message, String.valueOf(userId));
        log.info("==> 取关操作 MQ 发送成功, SendResult: {}", sendResult);
    }

    /**
     * ZSET 关注列表不存在时，从数据库全量回源同步到 Redis（含过期时间），供关注/取关复用。
     *
     * <p>回源保证后续校验基于完整数据。设随机过期时间（保底 1 天 + 随机秒）防雪崩。
     * 仅负责「查 DB + 全量回填 ZSET」，不含本次关注/取关的写入或移除——由调用方按业务补跑对应脚本。
     *
     * @param userId       当前用户 ID
     * @param followingKey 当前用户关注列表 ZSET Key
     * @return DB 关注记录是否为空（{@code true} 表示未关注任何人）
     */
    private boolean loadFollowingIntoRedis(Long userId, String followingKey) {
        // 查询数据库中当前用户的关注关系记录
        List<FollowingDO> followingDOS = followingDOMapper.selectList(
                new LambdaQueryWrapper<FollowingDO>().eq(FollowingDO::getUserId, userId));

        if (followingDOS == null || followingDOS.isEmpty()) {
            return true;
        }

        // 记录不为空：批量回源全量关注关系并设置随机过期时间
        Object[] batchArgs = buildBatchLuaArgs(followingDOS, randomFollowingExpireSeconds());
        stringRedisTemplate.execute(BATCH_ADD_AND_EXPIRE_SCRIPT,
                Collections.singletonList(followingKey), batchArgs);
        return false;
    }

    /**
     * 关注列表 ZSET 随机过期时间：保底 1 天 + [0, 1 天) 随机秒，打散过期防雪崩。
     */
    private static long randomFollowingExpireSeconds() {
        return FOLLOWING_EXPIRE_BASE_SECONDS
                + ThreadLocalRandom.current().nextInt((int) FOLLOWING_EXPIRE_BASE_SECONDS);
    }

    /**
     * 校验 Lua 脚本结果，对「达上限」「已关注」抛出对应业务异常。
     * ZSET 不存在（-1）与成功（0）不在此处理。
     */
    private static void checkLuaScriptResult(Long result) {
        LuaResultEnum luaResultEnum = LuaResultEnum.valueOf(result);
        if (Objects.isNull(luaResultEnum)) {
            throw new BizException(ResponseCodeEnum.SYSTEM_ERROR);
        }
        switch (luaResultEnum) {
            case FOLLOW_LIMIT -> throw new BizException(ResponseCodeEnum.FOLLOWING_COUNT_LIMIT);
            case ALREADY_FOLLOWED -> throw new BizException(ResponseCodeEnum.ALREADY_FOLLOWED);
            default -> {
                // ZSET_NOT_EXIST / FOLLOW_SUCCESS 无需处理
            }
        }
    }

    /**
     * 构建批量回源 Lua 脚本参数：[score1, member1, score2, member2, ..., expireSeconds]。
     * 全部转为字符串，配合 StringRedisTemplate 避免序列化污染 Lua 入参。
     */
    private static Object[] buildBatchLuaArgs(List<FollowingDO> followingDOS, long expireSeconds) {
        Object[] args = new Object[followingDOS.size() * 2 + 1];
        int i = 0;
        for (FollowingDO following : followingDOS) {
            args[i++] = String.valueOf(DateUtils.localDateTime2Timestamp(following.getCreateTime()));
            args[i++] = String.valueOf(following.getFollowingUserId());
        }
        args[args.length - 1] = String.valueOf(expireSeconds);
        return args;
    }

    /**
     * 构建粉丝列表批量回源 Lua 脚本参数：[score1, member1, ..., expireSeconds]。
     * score 取粉丝的关注时间戳，member 取粉丝的用户 ID；全部转为字符串。
     */
    private static Object[] buildFansZSetLuaArgs(List<FansDO> fansDOS, long expireSeconds) {
        Object[] args = new Object[fansDOS.size() * 2 + 1];
        int i = 0;
        for (FansDO fans : fansDOS) {
            args[i++] = String.valueOf(DateUtils.localDateTime2Timestamp(fans.getCreateTime()));
            args[i++] = String.valueOf(fans.getFansUserId());
        }
        args[args.length - 1] = String.valueOf(expireSeconds);
        return args;
    }

    // ===== 预编译 Lua 脚本（避免每次请求重复读取脚本文件） =====

    private static final DefaultRedisScript<Long> CHECK_AND_ADD_SCRIPT = buildLongScript("/lua/follow_check_and_add.lua");
    private static final DefaultRedisScript<Long> ADD_AND_EXPIRE_SCRIPT = buildLongScript("/lua/follow_add_and_expire.lua");
    private static final DefaultRedisScript<Long> BATCH_ADD_AND_EXPIRE_SCRIPT = buildLongScript("/lua/follow_batch_add_and_expire.lua");
    private static final DefaultRedisScript<Long> UNFOLLOW_CHECK_AND_DELETE_SCRIPT = buildLongScript("/lua/unfollow_check_and_delete.lua");

    private static DefaultRedisScript<Long> buildLongScript(String path) {
        DefaultRedisScript<Long> script = new DefaultRedisScript<>();
        script.setScriptSource(new ResourceScriptSource(new ClassPathResource(path)));
        script.setResultType(Long.class);
        return script;
    }
}
