package com.hanserwei.relation.consumer;

import com.google.common.util.concurrent.RateLimiter;
import com.hanserwei.framework.common.util.DateUtils;
import com.hanserwei.framework.common.util.JsonUtils;
import com.hanserwei.relation.constant.MQConstants;
import com.hanserwei.relation.constant.RedisKeyConstants;
import com.hanserwei.relation.config.RelationProperties;
import com.hanserwei.relation.domain.dataobject.FansDO;
import com.hanserwei.relation.domain.dataobject.FollowingDO;
import com.hanserwei.relation.domain.mapper.FansDOMapper;
import com.hanserwei.relation.domain.mapper.FollowingDOMapper;
import com.hanserwei.relation.enums.FollowUnfollowTypeEnum;
import com.hanserwei.relation.model.dto.CountFollowUnfollowMqDTO;
import com.hanserwei.relation.model.dto.FollowUserMqDTO;
import com.hanserwei.relation.model.dto.UnfollowUserMqDTO;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.client.producer.SendCallback;
import org.apache.rocketmq.client.producer.SendResult;
import org.apache.rocketmq.common.message.MessageExt;
import org.apache.rocketmq.spring.annotation.ConsumeMode;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.springframework.core.io.ClassPathResource;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.dao.RecoverableDataAccessException;
import org.springframework.dao.TransientDataAccessException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.scripting.support.ResourceScriptSource;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Objects;

/**
 * 关注、取关 MQ 消费者（集群模式）.
 *
 * <p>消费 {@link MQConstants#TOPIC_FOLLOW_OR_UNFOLLOW}，按 Tag 分派：{@code Follow} 落库
 * 关注关系并更新粉丝 ZSET，{@code Unfollow} 留待后续。集群模式下同组仅一个实例消费某条消息。
 *
 * <p>削峰：Guava 令牌桶 {@link RateLimiter} 阻塞式获取令牌，以数据库可承受速率消费。
 * 幂等：{@code t_following}/{@code t_fans} 联合唯一索引兜底，重复消费转成功不重试。
 *
 * @author hanserwei
 * @date 2026/07/10
 * @since 0.0.1
 */
@Slf4j
@Component
@RequiredArgsConstructor
@RocketMQMessageListener(
        consumerGroup = MQConstants.GROUP_FOLLOW_UNFOLLOW_CONSUMER,
        topic = MQConstants.TOPIC_FOLLOW_OR_UNFOLLOW,
        consumeMode = ConsumeMode.ORDERLY)
public class FollowUnfollowConsumer implements RocketMQListener<MessageExt> {

    private final FollowingDOMapper followingDOMapper;
    private final FansDOMapper fansDOMapper;
    private final TransactionTemplate transactionTemplate;
    private final StringRedisTemplate stringRedisTemplate;
    private final RelationProperties relationProperties;
    private final RateLimiter followUnfollowRateLimiter;
    private final RocketMQTemplate rocketMQTemplate;

    /** 更新粉丝 ZSET 的 Lua 脚本 */
    private static final DefaultRedisScript<Long> FANS_ZSET_SCRIPT = buildFansZsetScript();

    @Override
    public void onMessage(MessageExt message) {
        // 流量削峰：获取令牌，无可用令牌时阻塞直到获得
        followUnfollowRateLimiter.acquire();

        String bodyJsonStr = new String(message.getBody(), StandardCharsets.UTF_8);
        String tags = message.getTags();
        log.info("==> FollowUnfollowConsumer 消费消息: {}, tags: {}", bodyJsonStr, tags);

        // 按 Tag 分派操作类型
        if (Objects.equals(tags, MQConstants.TAG_FOLLOW)) {
            handleFollowTagMessage(bodyJsonStr);
        } else if (Objects.equals(tags, MQConstants.TAG_UNFOLLOW)) {
            handleUnfollowTagMessage(bodyJsonStr);
        }
    }

    /**
     * 处理关注消息：编程式事务双表落库，成功后增量更新被关注用户的粉丝 ZSET。
     *
     * @param bodyJsonStr 消息体 JSON
     */
    private void handleFollowTagMessage(String bodyJsonStr) {
        FollowUserMqDTO followUserMqDTO = JsonUtils.parseObject(bodyJsonStr, FollowUserMqDTO.class);
        if (Objects.isNull(followUserMqDTO)) {
            return;
        }

        Long userId = followUserMqDTO.getUserId();
        Long followUserId = followUserMqDTO.getFollowUserId();
        LocalDateTime createTime = followUserMqDTO.getCreateTime();

        // 编程式事务：关注表 + 粉丝表两条记录要么都成功，要么都回滚
        boolean isSuccess = Boolean.TRUE.equals(transactionTemplate.execute(status -> {
            try {
                followingDOMapper.insert(FollowingDO.builder()
                        .userId(userId)
                        .followingUserId(followUserId)
                        .createTime(createTime)
                        .build());
                fansDOMapper.insert(FansDO.builder()
                        .userId(followUserId)
                        .fansUserId(userId)
                        .createTime(createTime)
                        .build());
                return true;
            } catch (DuplicateKeyException e) {
                // 幂等：联合唯一索引冲突说明消息重复消费，视为成功，不回滚不重试
                log.info("==> 关注关系已存在（重复消费），userId: {}, followUserId: {}", userId, followUserId);
                return true;
            } catch (Exception e) {
                status.setRollbackOnly();
                // 瞬时故障（网络抖动/连接中断/超时/死锁）：抛出，交由 ORDERLY 挂起队列保序重试，DB 恢复后自愈
                rethrowIfTransient(e);
                // 毒丸消息（脏数据/映射错误等确定性异常）：重试永远失败，吞掉防止阻塞队列，靠 TTL 回源兜底
                log.error("==> 关注关系落库失败（非瞬时，不重试）, userId: {}, followUserId: {}", userId, followUserId, e);
                return false;
            }
        }));
        log.info("==> 关注关系落库结果: {}", isSuccess);

        // 落库成功才更新粉丝 ZSET（避免缓存与库不一致）
        if (isSuccess) {
            updateFansZset(userId, followUserId, createTime);
            // 通知计数服务：关注数（userId 关注数 +1）、粉丝数（followUserId 粉丝数 +1）
            sendCountMQ(userId, followUserId, FollowUnfollowTypeEnum.FOLLOW.getCode());
        }
    }

    /**
     * 处理取关消息：编程式事务删除双表记录，成功后从被取关方粉丝 ZSET 移除发起者。
     *
     * @param bodyJsonStr 消息体 JSON
     */
    private void handleUnfollowTagMessage(String bodyJsonStr) {
        UnfollowUserMqDTO unfollowUserMqDTO = JsonUtils.parseObject(bodyJsonStr, UnfollowUserMqDTO.class);
        if (Objects.isNull(unfollowUserMqDTO)) {
            return;
        }

        Long userId = unfollowUserMqDTO.getUserId();
        Long unfollowUserId = unfollowUserMqDTO.getUnfollowUserId();

        // 编程式事务：关注表 + 粉丝表两条记录要么都删除，要么都回滚
        boolean isSuccess = Boolean.TRUE.equals(transactionTemplate.execute(status -> {
            try {
                // 关注表：删除「我关注对方」这条记录
                int count = followingDOMapper.delete(new LambdaQueryWrapper<FollowingDO>()
                        .eq(FollowingDO::getUserId, userId)
                        .eq(FollowingDO::getFollowingUserId, unfollowUserId));
                // 删成功再删粉丝表：从对方的粉丝里移除我
                if (count > 0) {
                    fansDOMapper.delete(new LambdaQueryWrapper<FansDO>()
                            .eq(FansDO::getUserId, unfollowUserId)
                            .eq(FansDO::getFansUserId, userId));
                }
                return true;
            } catch (Exception e) {
                status.setRollbackOnly();
                // 瞬时故障：抛出触发 ORDERLY 保序重试；毒丸消息吞掉防止阻塞队列
                rethrowIfTransient(e);
                log.error("==> 取关关系删库失败（非瞬时，不重试）, userId: {}, unfollowUserId: {}", userId, unfollowUserId, e);
                return false;
            }
        }));
        log.info("==> 取关关系删库结果: {}", isSuccess);

        // 删库成功后，从被取关方的粉丝 ZSET 移除发起者（保证缓存与库一致）
        if (isSuccess) {
            String fansKey = RedisKeyConstants.buildUserFansKey(unfollowUserId);
            stringRedisTemplate.opsForZSet().remove(fansKey, String.valueOf(userId));
            // 通知计数服务：关注数（userId 关注数 -1）、粉丝数（unfollowUserId 粉丝数 -1）
            sendCountMQ(userId, unfollowUserId, FollowUnfollowTypeEnum.UNFOLLOW.getCode());
        }
    }

    /**
     * 瞬时/可恢复类数据访问异常则重新抛出，交由上层（RocketMQ ORDERLY）挂起当前队列、保序重试。
     *
     * <p>覆盖网络抖动、连接中途断开、语句超时、死锁/锁等待等——这类异常重试大概率能成，
     * 顺序消费"卡住等 DB 恢复"正是期望行为。其余确定性异常（完整性冲突、SQL/映射错误等毒丸）
     * 不在此抛出，由调用方吞掉，避免同一条脏消息把队列永久堵死（ORDERLY 默认近乎无限重试）。
     *
     * @param e 事务执行中捕获的异常
     */
    private static void rethrowIfTransient(Exception e) {
        if (e instanceof TransientDataAccessException || e instanceof RecoverableDataAccessException) {
            throw (RuntimeException) e;
        }
    }

    /**
     * 增量更新被关注用户的粉丝 ZSET（仅当 ZSET 已初始化时生效）。
     *
     * @param userId       粉丝（发起关注者）ID
     * @param followUserId 被关注用户 ID（粉丝 ZSET 的属主）
     * @param createTime   关注时间
     */
    private void updateFansZset(Long userId, Long followUserId, LocalDateTime createTime) {
        String fansKey = RedisKeyConstants.buildUserFansKey(followUserId);
        long timestamp = DateUtils.localDateTime2Timestamp(createTime);
        int maxCacheCount = relationProperties.getFans().getMaxCacheCount();

        stringRedisTemplate.execute(FANS_ZSET_SCRIPT,
                Collections.singletonList(fansKey),
                String.valueOf(userId), String.valueOf(timestamp), String.valueOf(maxCacheCount));
    }

    /**
     * 通知计数服务：发送 2 条计数 MQ（关注数 + 粉丝数）。
     *
     * <p>关注数与粉丝数由计数服务的不同消费者独立统计，故拆成两个 topic 各发一条，
     * 消息体一致（同一 {@link CountFollowUnfollowMqDTO}）。异步发送，不阻塞消费主流程。
     *
     * @param userId       原用户 ID（发起关注/取关者）
     * @param targetUserId 目标用户 ID（被关注/被取关者）
     * @param type         操作类型：1 关注，0 取关
     */
    private void sendCountMQ(Long userId, Long targetUserId, Integer type) {
        CountFollowUnfollowMqDTO dto = CountFollowUnfollowMqDTO.builder()
                .userId(userId)
                .targetUserId(targetUserId)
                .type(type)
                .build();
        org.springframework.messaging.Message<String> message =
                MessageBuilder.withPayload(JsonUtils.toJsonString(dto)).build();

        // 关注数计数
        rocketMQTemplate.asyncSend(MQConstants.TOPIC_COUNT_FOLLOWING, message, new SendCallback() {
            @Override
            public void onSuccess(SendResult sendResult) {
                log.info("==> 【计数服务：关注数】MQ 发送成功, SendResult: {}", sendResult);
            }

            @Override
            public void onException(Throwable throwable) {
                log.error("==> 【计数服务：关注数】MQ 发送异常: ", throwable);
            }
        });

        // 粉丝数计数
        rocketMQTemplate.asyncSend(MQConstants.TOPIC_COUNT_FANS, message, new SendCallback() {
            @Override
            public void onSuccess(SendResult sendResult) {
                log.info("==> 【计数服务：粉丝数】MQ 发送成功, SendResult: {}", sendResult);
            }

            @Override
            public void onException(Throwable throwable) {
                log.error("==> 【计数服务：粉丝数】MQ 发送异常: ", throwable);
            }
        });
    }

    private static DefaultRedisScript<Long> buildFansZsetScript() {
        DefaultRedisScript<Long> script = new DefaultRedisScript<>();
        script.setScriptSource(new ResourceScriptSource(
                new ClassPathResource("/lua/follow_check_and_update_fans_zset.lua")));
        script.setResultType(Long.class);
        return script;
    }
}
