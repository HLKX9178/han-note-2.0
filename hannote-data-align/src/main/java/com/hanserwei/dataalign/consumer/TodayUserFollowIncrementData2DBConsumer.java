package com.hanserwei.dataalign.consumer;

import com.hanserwei.dataalign.config.TableShardProperties;
import com.hanserwei.dataalign.constant.MQConstants;
import com.hanserwei.dataalign.constant.RedisKeyConstants;
import com.hanserwei.dataalign.constant.TableConstants;
import com.hanserwei.dataalign.domain.mapper.InsertMapper;
import com.hanserwei.dataalign.model.dto.FollowUnfollowMqDTO;
import com.hanserwei.dataalign.util.BloomFilterExecutor;
import com.hanserwei.framework.common.util.JsonUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Objects;

/**
 * 日增量落库：用户关注 / 取关.
 *
 * <p>消费 {@code CountFollowingTopic}，对应两个变更维度：源用户的关注数（userId）与
 * 目标用户的粉丝数（targetUserId），各自独立布隆判重、独立落库。
 *
 * @author hanserwei
 * @date 2026/07/11
 * @since 0.0.1
 */
@Slf4j
@Component
@RequiredArgsConstructor
@RocketMQMessageListener(
        consumerGroup = MQConstants.GROUP_DATA_ALIGN_FOLLOWING,
        topic = MQConstants.TOPIC_COUNT_FOLLOWING)
public class TodayUserFollowIncrementData2DBConsumer implements RocketMQListener<String> {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd");

    private final InsertMapper insertMapper;
    private final BloomFilterExecutor bloomFilterExecutor;
    private final TableShardProperties tableShardProperties;

    @Override
    public void onMessage(String body) {
        log.info("## TodayUserFollowIncrementData2DBConsumer 消费到 MQ: {}", body);

        FollowUnfollowMqDTO dto = JsonUtils.parseObject(body, FollowUnfollowMqDTO.class);
        if (Objects.isNull(dto)) {
            return;
        }

        Long userId = dto.getUserId();
        Long targetUserId = dto.getTargetUserId();
        String date = LocalDate.now().format(DATE_FORMATTER);
        int shards = tableShardProperties.getShards();

        // ---------- 源用户：关注数变更 ----------
        if (Objects.nonNull(userId)) {
            String followBloomKey = RedisKeyConstants.buildBloomUserFollowKey(date);
            if (bloomFilterExecutor.isAbsent(followBloomKey, userId)) {
                try {
                    insertMapper.insertFollowingCountTemp(
                            TableConstants.buildTableNameSuffix(date, userId % shards), userId);
                    bloomFilterExecutor.add(followBloomKey, userId);
                } catch (Exception e) {
                    log.error("## 落库用户关注数日增量失败, userId={}", userId, e);
                }
            }
        }

        // ---------- 目标用户：粉丝数变更 ----------
        if (Objects.nonNull(targetUserId)) {
            String fansBloomKey = RedisKeyConstants.buildBloomUserFansKey(date);
            if (bloomFilterExecutor.isAbsent(fansBloomKey, targetUserId)) {
                try {
                    insertMapper.insertFansCountTemp(
                            TableConstants.buildTableNameSuffix(date, targetUserId % shards), targetUserId);
                    bloomFilterExecutor.add(fansBloomKey, targetUserId);
                } catch (Exception e) {
                    log.error("## 落库用户粉丝数日增量失败, targetUserId={}", targetUserId, e);
                }
            }
        }
    }
}
