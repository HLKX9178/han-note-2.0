package com.hanserwei.count.consumer;

import com.google.common.util.concurrent.RateLimiter;
import com.hanserwei.count.constant.MQConstants;
import com.hanserwei.count.domain.mapper.NoteCountDOMapper;
import com.hanserwei.count.domain.mapper.UserCountDOMapper;
import com.hanserwei.count.model.dto.AggregationCountNoteMqDTO;
import com.hanserwei.framework.common.util.JsonUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.List;
import java.util.Objects;

/**
 * 计数：笔记收藏数落库消费者.
 *
 * <p>Guava 令牌桶（5000/s）削峰后，编程式事务同时更新 {@code t_note_count.collect_total}
 * 与 {@code t_user_count.collect_total}（用户维度获藏数）。消息体为聚合后的 List JSON。
 *
 * @author hanserwei
 * @date 2026/07/11
 * @since 0.0.1
 */
@Slf4j
@Component
@RequiredArgsConstructor
@RocketMQMessageListener(
        consumerGroup = MQConstants.GROUP_COUNT_NOTE_COLLECT_2_DB,
        topic = MQConstants.TOPIC_COUNT_NOTE_COLLECT_2_DB)
public class CountNoteCollect2DBConsumer implements RocketMQListener<String> {

    private final NoteCountDOMapper noteCountDOMapper;
    private final UserCountDOMapper userCountDOMapper;
    private final TransactionTemplate transactionTemplate;

    /** 每秒 5000 个令牌，写库限流削峰 */
    private final RateLimiter rateLimiter = RateLimiter.create(5000);

    @Override
    public void onMessage(String body) {
        rateLimiter.acquire();

        log.info("## 消费到 MQ 【计数：笔记收藏数入库】: {}", body);
        if (StringUtils.isBlank(body)) {
            return;
        }

        List<AggregationCountNoteMqDTO> countList = null;
        try {
            countList = JsonUtils.parseList(body, AggregationCountNoteMqDTO.class);
        } catch (Exception e) {
            log.error("## 解析笔记收藏数计数 JSON 失败: {}", body, e);
        }
        if (countList == null || countList.isEmpty()) {
            return;
        }

        countList.forEach(item -> {
            Long noteId = item.getNoteId();
            Long creatorId = item.getCreatorId();
            Integer count = item.getCount();

            transactionTemplate.execute(status -> {
                try {
                    noteCountDOMapper.insertOrUpdateCollectTotalByNoteId(count, noteId);
                    if (Objects.nonNull(creatorId)) {
                        userCountDOMapper.insertOrUpdateCollectTotalByUserId(count, creatorId);
                    }
                    return true;
                } catch (Exception e) {
                    status.setRollbackOnly();
                    log.error("## 笔记收藏数落库失败, noteId: {}, creatorId: {}", noteId, creatorId, e);
                    return false;
                }
            });
        });
    }
}
