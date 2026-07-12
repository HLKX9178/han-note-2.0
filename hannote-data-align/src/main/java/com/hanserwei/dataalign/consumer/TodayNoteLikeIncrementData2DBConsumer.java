package com.hanserwei.dataalign.consumer;

import com.hanserwei.dataalign.config.TableShardProperties;
import com.hanserwei.dataalign.constant.MQConstants;
import com.hanserwei.dataalign.constant.RedisKeyConstants;
import com.hanserwei.dataalign.constant.TableConstants;
import com.hanserwei.dataalign.domain.mapper.InsertMapper;
import com.hanserwei.dataalign.model.dto.LikeUnlikeNoteMqDTO;
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
 * 日增量落库：笔记点赞 / 取消点赞.
 *
 * <p>消费 {@code CountNoteLikeTopic}，记录当日发生变更、需重新对齐的两个维度：
 * 笔记被点赞数（noteId）与发布者获赞数（noteCreatorId）。
 *
 * <p>10418 修复：noteId、noteCreatorId <strong>各用独立布隆</strong>、各自独立落库、不加事务，
 * 避免「同一作者两篇笔记先后被点赞」时因唯一约束致第二次插入回滚。
 *
 * @author hanserwei
 * @date 2026/07/11
 * @since 0.0.1
 */
@Slf4j
@Component
@RequiredArgsConstructor
@RocketMQMessageListener(
        consumerGroup = MQConstants.GROUP_DATA_ALIGN_NOTE_LIKE,
        topic = MQConstants.TOPIC_COUNT_NOTE_LIKE)
public class TodayNoteLikeIncrementData2DBConsumer implements RocketMQListener<String> {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd");

    private final InsertMapper insertMapper;
    private final BloomFilterExecutor bloomFilterExecutor;
    private final TableShardProperties tableShardProperties;

    @Override
    public void onMessage(String body) {
        log.info("## TodayNoteLikeIncrementData2DBConsumer 消费到 MQ: {}", body);

        LikeUnlikeNoteMqDTO dto = JsonUtils.parseObject(body, LikeUnlikeNoteMqDTO.class);
        if (Objects.isNull(dto)) {
            return;
        }

        Long noteId = dto.getNoteId();
        Long noteCreatorId = dto.getNoteCreatorId();
        String date = LocalDate.now().format(DATE_FORMATTER);
        int shards = tableShardProperties.getShards();

        // ---------- 笔记维度：被点赞数变更 ----------
        if (Objects.nonNull(noteId)) {
            String noteBloomKey = RedisKeyConstants.buildBloomNoteLikeNoteIdKey(date);
            if (bloomFilterExecutor.isAbsent(noteBloomKey, noteId)) {
                try {
                    insertMapper.insertNoteLikeCountTemp(
                            TableConstants.buildTableNameSuffix(date, noteId % shards), noteId);
                    bloomFilterExecutor.add(noteBloomKey, noteId);
                } catch (Exception e) {
                    log.error("## 落库笔记点赞数日增量失败, noteId={}", noteId, e);
                }
            }
        }

        // ---------- 用户维度：发布者获赞数变更 ----------
        if (Objects.nonNull(noteCreatorId)) {
            String userBloomKey = RedisKeyConstants.buildBloomNoteLikeUserIdKey(date);
            if (bloomFilterExecutor.isAbsent(userBloomKey, noteCreatorId)) {
                try {
                    insertMapper.insertUserLikeCountTemp(
                            TableConstants.buildTableNameSuffix(date, noteCreatorId % shards), noteCreatorId);
                    bloomFilterExecutor.add(userBloomKey, noteCreatorId);
                } catch (Exception e) {
                    log.error("## 落库用户获赞数日增量失败, noteCreatorId={}", noteCreatorId, e);
                }
            }
        }
    }
}
