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
 * <p>消费 {@link CountNoteCommentConsumer} 转发的聚合结果
 * （{@link MQConstants#TOPIC_COUNT_NOTE_COMMENT_2_DB}），把每个笔记的评论数净增量
 * 以 upsert 方式累加进 {@code t_note_count}，实现评论计数最终一致落库。
 *
 * <p>落库前经 {@link RateLimiter} 限流（5000 QPS），保护数据库在评论洪峰下不被打垮。
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
    /** 落库限流器：控制评论洪峰下写库 QPS，保护数据库 */
    private final RateLimiter rateLimiter = RateLimiter.create(5000);

    /**
     * 消费聚合结果并累加落库.
     *
     * @param body 聚合后的评论计数列表 JSON（{@code List<AggregationCountNoteMqDTO>}）
     */
    @Override
    public void onMessage(String body) {
        if (StringUtils.isBlank(body)) {
            return;
        }
        // 反序列化聚合结果，空批直接返回
        List<AggregationCountNoteMqDTO> counts = JsonUtils.parseList(body, AggregationCountNoteMqDTO.class);
        if (counts == null || counts.isEmpty()) {
            return;
        }
        // 按本批条数申请令牌，超出速率则阻塞等待
        rateLimiter.acquire(counts.size());
        // 逐笔 upsert 累加评论总数增量
        counts.forEach(item -> noteCountDOMapper.insertOrUpdateCommentTotalByNoteId(
                item.getCount(), item.getNoteId()));
    }
}
