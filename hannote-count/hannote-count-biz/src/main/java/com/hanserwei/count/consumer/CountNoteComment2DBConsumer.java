package com.hanserwei.count.consumer;

import com.google.common.util.concurrent.RateLimiter;
import com.hanserwei.count.constant.MQConstants;
import com.hanserwei.count.domain.mapper.NoteCountDOMapper;
import com.hanserwei.count.model.dto.AggregationCountNoteMqDTO;
import com.hanserwei.framework.common.util.JsonUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 聚合后的笔记评论总数落库消费者.
 *
 * @author hanserwei
 * @date 2026/07/15
 * @since 0.0.1
 */
@Slf4j
@Component
@RequiredArgsConstructor
@RocketMQMessageListener(
        consumerGroup = MQConstants.GROUP_COUNT_NOTE_COMMENT_2_DB,
        topic = MQConstants.TOPIC_COUNT_NOTE_COMMENT_2_DB)
public class CountNoteComment2DBConsumer implements RocketMQListener<String> {

    private final NoteCountDOMapper noteCountDOMapper;
    private final RateLimiter rateLimiter = RateLimiter.create(5000);

    @Override
    public void onMessage(String body) {
        if (StringUtils.isBlank(body)) {
            return;
        }
        List<AggregationCountNoteMqDTO> counts = JsonUtils.parseList(body, AggregationCountNoteMqDTO.class);
        if (counts == null || counts.isEmpty()) {
            return;
        }
        rateLimiter.acquire(counts.size());
        counts.forEach(item -> noteCountDOMapper.insertOrUpdateCommentTotalByNoteId(
                item.getCount(), item.getNoteId()));
    }
}
