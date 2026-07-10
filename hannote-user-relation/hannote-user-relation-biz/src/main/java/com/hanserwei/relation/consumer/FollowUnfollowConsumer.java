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
import com.hanserwei.relation.model.dto.FollowUserMqDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.common.message.MessageExt;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.core.io.ClassPathResource;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
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
        topic = MQConstants.TOPIC_FOLLOW_OR_UNFOLLOW)
public class FollowUnfollowConsumer implements RocketMQListener<MessageExt> {

    private final FollowingDOMapper followingDOMapper;
    private final FansDOMapper fansDOMapper;
    private final TransactionTemplate transactionTemplate;
    private final StringRedisTemplate stringRedisTemplate;
    private final RelationProperties relationProperties;
    private final RateLimiter followUnfollowRateLimiter;

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
            // TODO: 取关落库（复刻后续博客）
            log.info("==> 暂未实现取关消费逻辑, tags: {}", tags);
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
                log.error("==> 关注关系落库失败, userId: {}, followUserId: {}", userId, followUserId, e);
                return false;
            }
        }));
        log.info("==> 关注关系落库结果: {}", isSuccess);

        // 落库成功才更新粉丝 ZSET（避免缓存与库不一致）
        if (isSuccess) {
            updateFansZset(userId, followUserId, createTime);
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

    private static DefaultRedisScript<Long> buildFansZsetScript() {
        DefaultRedisScript<Long> script = new DefaultRedisScript<>();
        script.setScriptSource(new ResourceScriptSource(
                new ClassPathResource("/lua/follow_check_and_update_fans_zset.lua")));
        script.setResultType(Long.class);
        return script;
    }
}
