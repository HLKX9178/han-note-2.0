package com.hanserwei.relation.service.impl;

import com.hanserwei.framework.biz.context.holder.LoginUserContextHolder;
import com.hanserwei.framework.common.exception.BizException;
import com.hanserwei.framework.common.response.Response;
import com.hanserwei.framework.common.util.DateUtils;
import com.hanserwei.framework.common.util.JsonUtils;
import com.hanserwei.relation.config.RelationProperties;
import com.hanserwei.relation.constant.MQConstants;
import com.hanserwei.relation.model.dto.FollowUserMqDTO;
import com.hanserwei.relation.constant.RedisKeyConstants;
import com.hanserwei.relation.domain.dataobject.FollowingDO;
import com.hanserwei.relation.domain.mapper.FollowingDOMapper;
import com.hanserwei.relation.enums.LuaResultEnum;
import com.hanserwei.relation.enums.ResponseCodeEnum;
import com.hanserwei.relation.model.vo.FollowUserReqVO;
import com.hanserwei.relation.rpc.UserRpcService;
import com.hanserwei.relation.service.RelationService;
import com.hanserwei.user.api.dto.resp.FindUserByIdRspDTO;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.client.producer.SendCallback;
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
    private final RelationProperties relationProperties;
    private final RocketMQTemplate rocketMQTemplate;

    /** 关注列表 ZSET 保底过期时间：1 天（秒） */
    private static final long FOLLOWING_EXPIRE_BASE_SECONDS = 60 * 60 * 24;

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
            syncFollowingFromDbThenAdd(userId, followUserId, followingKey, timestamp, maxLimit);
        }

        // 5. 发送 MQ，由消费者异步落库 t_following + t_fans（削峰、提升接口响应）
        sendFollowMq(userId, followUserId, now);

        return Response.success();
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

        // 异步发送，不阻塞接口响应
        rocketMQTemplate.asyncSend(destination, message, new SendCallback() {
            @Override
            public void onSuccess(SendResult sendResult) {
                log.info("==> 关注操作 MQ 发送成功, SendResult: {}", sendResult);
            }

            @Override
            public void onException(Throwable throwable) {
                log.error("==> 关注操作 MQ 发送异常: ", throwable);
            }
        });
    }

    /**
     * ZSET 关注列表不存在时，从数据库全量回源同步，再写入本次关注关系。
     *
     * <p>回源保证后续 ZCARD 上限校验基于完整数据。设随机过期时间（保底 1 天 + 随机秒）防雪崩。
     * 注意：本期数据库尚无关注记录（落库在 MQ 消费者章节），故实际走「记录为空」分支；
     * 回源分支代码先行建好，落库上线后自动生效。
     */
    private void syncFollowingFromDbThenAdd(Long userId, Long followUserId,
                                            String followingKey, long timestamp, int maxLimit) {
        // 查询数据库中当前用户的关注关系记录
        List<FollowingDO> followingDOS = followingDOMapper.selectList(
                new LambdaQueryWrapper<FollowingDO>().eq(FollowingDO::getUserId, userId));

        // 随机过期时间：保底 1 天 + [0, 1 天) 随机秒
        long expireSeconds = FOLLOWING_EXPIRE_BASE_SECONDS
                + ThreadLocalRandom.current().nextInt((int) FOLLOWING_EXPIRE_BASE_SECONDS);

        if (followingDOS == null || followingDOS.isEmpty()) {
            // 记录为空：直接写入首条关注关系并设置过期时间
            stringRedisTemplate.execute(ADD_AND_EXPIRE_SCRIPT,
                    Collections.singletonList(followingKey),
                    String.valueOf(followUserId), String.valueOf(timestamp), String.valueOf(expireSeconds));
        } else {
            // 记录不为空：批量回源全量关注关系并设置过期时间
            Object[] batchArgs = buildBatchLuaArgs(followingDOS, expireSeconds);
            stringRedisTemplate.execute(BATCH_ADD_AND_EXPIRE_SCRIPT,
                    Collections.singletonList(followingKey), batchArgs);

            // 回源后重新执行校验并写入本次关注
            Long retry = stringRedisTemplate.execute(CHECK_AND_ADD_SCRIPT,
                    Collections.singletonList(followingKey),
                    String.valueOf(followUserId), String.valueOf(timestamp), String.valueOf(maxLimit));
            checkLuaScriptResult(retry);
        }
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

    // ===== 预编译 Lua 脚本（避免每次请求重复读取脚本文件） =====

    private static final DefaultRedisScript<Long> CHECK_AND_ADD_SCRIPT = buildLongScript("/lua/follow_check_and_add.lua");
    private static final DefaultRedisScript<Long> ADD_AND_EXPIRE_SCRIPT = buildLongScript("/lua/follow_add_and_expire.lua");
    private static final DefaultRedisScript<Long> BATCH_ADD_AND_EXPIRE_SCRIPT = buildLongScript("/lua/follow_batch_add_and_expire.lua");

    private static DefaultRedisScript<Long> buildLongScript(String path) {
        DefaultRedisScript<Long> script = new DefaultRedisScript<>();
        script.setScriptSource(new ResourceScriptSource(new ClassPathResource(path)));
        script.setResultType(Long.class);
        return script;
    }
}
