package com.hanserwei.relation.consumer;

import cn.hutool.core.collection.CollUtil;
import com.google.common.util.concurrent.RateLimiter;
import com.hanserwei.framework.common.util.DateUtils;
import com.hanserwei.framework.common.util.InteractionMergeSupport;
import com.hanserwei.framework.common.util.JsonUtils;
import com.hanserwei.relation.config.RelationProperties;
import com.hanserwei.relation.constant.MQConstants;
import com.hanserwei.relation.constant.RedisKeyConstants;
import com.hanserwei.relation.domain.dataobject.FansDO;
import com.hanserwei.relation.domain.dataobject.FollowingDO;
import com.hanserwei.relation.domain.mapper.FansDOMapper;
import com.hanserwei.relation.domain.mapper.FollowingDOMapper;
import com.hanserwei.relation.enums.FollowUnfollowTypeEnum;
import com.hanserwei.relation.model.dto.FollowUserMqDTO;
import com.hanserwei.relation.model.dto.UnfollowUserMqDTO;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.client.consumer.DefaultMQPushConsumer;
import org.apache.rocketmq.client.consumer.listener.ConsumeOrderlyStatus;
import org.apache.rocketmq.client.consumer.listener.MessageListenerOrderly;
import org.apache.rocketmq.client.exception.MQClientException;
import org.apache.rocketmq.common.consumer.ConsumeFromWhere;
import org.apache.rocketmq.common.message.MessageExt;
import org.apache.rocketmq.remoting.protocol.heartbeat.MessageModel;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.scripting.support.ResourceScriptSource;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * 关注 / 取关 MQ 消费者（rocketmq-client 原生批量顺序消费）.
 *
 * <p>批量顺序消费 {@link MQConstants#TOPIC_FOLLOW_OR_UNFOLLOW}：令牌桶削峰后，按 Tag
 * （{@link MQConstants#TAG_FOLLOW}/{@link MQConstants#TAG_UNFOLLOW}）解析为统一 {@link FollowOp}，
 * 对一批消息按 {@code (userId, targetUserId)} 做奇偶抵消合并（{@link InteractionMergeSupport}），
 * 再把最终操作拆成关注组 / 取关组分别批量落库：
 * <ul>
 *   <li>关注组：{@code t_following}、{@code t_fans} 各批量 insert（{@code ON CONFLICT DO NOTHING} 幂等）；</li>
 *   <li>取关组：{@code t_following}、{@code t_fans} 各批量 delete。</li>
 * </ul>
 * 双表写在同一编程式事务内保证原子性；事务提交后逐个执行粉丝 ZSET 副作用（follow 增量、unfollow 移除）。
 *
 * <p>顺序性由生产端按 {@code userId} hashKey 有序发送保证。计数已改由计数服务并行直消费源 Topic，
 * 本消费者不再转发计数 MQ。
 *
 * <p><b>与原单条版的行为差异（供 CR 关注）</b>：原按单条区分「瞬时故障重试 / 毒丸吞掉」；批量版对整批
 * 事务失败统一返回 {@code SUSPEND_CURRENT_QUEUE_A_MOMENT} 挂起重试，超过 {@code maxReconsumeTimes}
 * 后由 RocketMQ 转入死信队列。
 *
 * @author hanserwei
 * @date 2026/07/14
 * @since 0.0.1
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class FollowUnfollowConsumer {

    @org.springframework.beans.factory.annotation.Value("${rocketmq.name-server}")
    private String namesrvAddr;

    private final FollowingDOMapper followingDOMapper;
    private final FansDOMapper fansDOMapper;
    private final TransactionTemplate transactionTemplate;
    private final StringRedisTemplate stringRedisTemplate;
    private final RelationProperties relationProperties;
    private final RateLimiter followUnfollowRateLimiter;

    private DefaultMQPushConsumer consumer;

    /** 更新粉丝 ZSET 的 Lua 脚本 */
    private static final DefaultRedisScript<Long> FANS_ZSET_SCRIPT = buildFansZsetScript();

    /**
     * 归一化后的关注/取关操作.
     *
     * @param userId       发起者用户 ID
     * @param targetUserId 目标用户 ID（被关注/被取关者）
     * @param type         操作类型：1 关注 / 0 取关
     * @param createTime   操作时间
     */
    private record FollowOp(Long userId, Long targetUserId, Integer type, LocalDateTime createTime) {
    }

    @PostConstruct
    public void init() throws MQClientException {
        consumer = new DefaultMQPushConsumer(MQConstants.GROUP_FOLLOW_UNFOLLOW_CONSUMER);
        consumer.setNamesrvAddr(namesrvAddr);
        consumer.subscribe(MQConstants.TOPIC_FOLLOW_OR_UNFOLLOW, "*");
        consumer.setConsumeFromWhere(ConsumeFromWhere.CONSUME_FROM_LAST_OFFSET);
        consumer.setMessageModel(MessageModel.CLUSTERING);
        consumer.setConsumeMessageBatchMaxSize(30);
        consumer.setPullInterval(1000);
        consumer.setMaxReconsumeTimes(3);

        consumer.registerMessageListener((MessageListenerOrderly) (msgs, context) -> {
            log.info("==> 【关注/取关】本批次消息大小: {}", msgs.size());
            try {
                // 流量削峰（动态限流 Bean，可随扩缩容变速）
                followUnfollowRateLimiter.acquire();

                // 消息体按 Tag 归一化为 FollowOp
                List<FollowOp> ops = msgs.stream()
                        .map(this::toFollowOp)
                        .filter(Objects::nonNull)
                        .toList();

                // 内存合并：同 (userId, targetUserId) 偶数次抵消、奇数次取最后一次
                List<FollowOp> merged = InteractionMergeSupport.mergeByLastOp(
                        ops, FollowOp::userId, FollowOp::targetUserId);
                if (CollUtil.isEmpty(merged)) {
                    return ConsumeOrderlyStatus.SUCCESS;
                }

                // 拆关注组 / 取关组
                List<FollowOp> followOps = merged.stream()
                        .filter(op -> Objects.equals(op.type(), FollowUnfollowTypeEnum.FOLLOW.getCode()))
                        .toList();
                List<FollowOp> unfollowOps = merged.stream()
                        .filter(op -> Objects.equals(op.type(), FollowUnfollowTypeEnum.UNFOLLOW.getCode()))
                        .toList();

                // 编程式事务：双表批量写原子化
                transactionTemplate.executeWithoutResult(status -> {
                    if (CollUtil.isNotEmpty(followOps)) {
                        followingDOMapper.batchInsertIgnore(followOps.stream()
                                .map(op -> FollowingDO.builder()
                                        .userId(op.userId())
                                        .followingUserId(op.targetUserId())
                                        .createTime(op.createTime())
                                        .build())
                                .toList());
                        fansDOMapper.batchInsertIgnore(followOps.stream()
                                .map(op -> FansDO.builder()
                                        .userId(op.targetUserId())
                                        .fansUserId(op.userId())
                                        .createTime(op.createTime())
                                        .build())
                                .toList());
                    }
                    if (CollUtil.isNotEmpty(unfollowOps)) {
                        followingDOMapper.batchDelete(unfollowOps.stream()
                                .map(op -> FollowingDO.builder()
                                        .userId(op.userId())
                                        .followingUserId(op.targetUserId())
                                        .build())
                                .toList());
                        fansDOMapper.batchDelete(unfollowOps.stream()
                                .map(op -> FansDO.builder()
                                        .userId(op.targetUserId())
                                        .fansUserId(op.userId())
                                        .build())
                                .toList());
                    }
                });

                // 事务提交后处理粉丝 ZSET 副作用（保证缓存与库一致）
                followOps.forEach(op -> updateFansZset(op.userId(), op.targetUserId(), op.createTime()));
                unfollowOps.forEach(op -> {
                    String fansKey = RedisKeyConstants.buildUserFansKey(op.targetUserId());
                    stringRedisTemplate.opsForZSet().remove(fansKey, String.valueOf(op.userId()));
                });

                return ConsumeOrderlyStatus.SUCCESS;
            } catch (Exception e) {
                log.error("==> 【关注/取关】批量消费失败，挂起当前队列稍后重试: ", e);
                return ConsumeOrderlyStatus.SUSPEND_CURRENT_QUEUE_A_MOMENT;
            }
        });

        consumer.start();
        log.info("## FollowUnfollowConsumer 启动完成，批量顺序消费 {}", MQConstants.TOPIC_FOLLOW_OR_UNFOLLOW);
    }

    @PreDestroy
    public void destroy() {
        if (Objects.nonNull(consumer)) {
            try {
                consumer.shutdown();
            } catch (Exception e) {
                log.error("==> FollowUnfollowConsumer 关闭异常: ", e);
            }
        }
    }

    /**
     * 将一条源消息按 Tag 归一化为 {@link FollowOp}.
     *
     * @param message 源消息
     * @return 归一化操作；无法识别的 Tag 或解析失败返回 {@code null}
     */
    private FollowOp toFollowOp(MessageExt message) {
        String bodyJsonStr = new String(message.getBody(), StandardCharsets.UTF_8);
        String tags = message.getTags();
        if (Objects.equals(tags, MQConstants.TAG_FOLLOW)) {
            FollowUserMqDTO dto = JsonUtils.parseObject(bodyJsonStr, FollowUserMqDTO.class);
            if (Objects.isNull(dto)) {
                return null;
            }
            return new FollowOp(dto.getUserId(), dto.getFollowUserId(),
                    FollowUnfollowTypeEnum.FOLLOW.getCode(), dto.getCreateTime());
        } else if (Objects.equals(tags, MQConstants.TAG_UNFOLLOW)) {
            UnfollowUserMqDTO dto = JsonUtils.parseObject(bodyJsonStr, UnfollowUserMqDTO.class);
            if (Objects.isNull(dto)) {
                return null;
            }
            return new FollowOp(dto.getUserId(), dto.getUnfollowUserId(),
                    FollowUnfollowTypeEnum.UNFOLLOW.getCode(), dto.getCreateTime());
        }
        return null;
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
