package com.hanserwei.dataalign.consumer;

import com.hanserwei.dataalign.config.TableShardProperties;
import com.hanserwei.dataalign.constant.MQConstants;
import com.hanserwei.dataalign.constant.RedisKeyConstants;
import com.hanserwei.dataalign.constant.TableConstants;
import com.hanserwei.dataalign.domain.mapper.InsertMapper;
import com.hanserwei.dataalign.model.dto.CommentCountChangedMqDTO;
import com.hanserwei.dataalign.util.BloomFilterExecutor;
import com.hanserwei.framework.common.util.JsonUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * 日增量落库：笔记评论总数变更.
 *
 * <p>链路：评论真实新增/删除后发出 {@link MQConstants#TOPIC_COMMENT_COUNT_CHANGED} 消息，
 * 本消费者只负责「记录当日发生过变更的 noteId」——按 noteId 取模落入对应分片的当日临时表，
 * 供凌晨的分片对齐任务重算 {@code t_comment} 真实值、纠正 {@code t_note_count.comment_total}。
 *
 * <p>为避免同一 noteId 当日重复落库，先用当日布隆过滤器判重，仅首次出现才写表并加入布隆，
 * 把海量重复消息收敛为「每日每 noteId 一行」。
 *
 * @author hanserwei
 * @date 2026/07/15
 * @since 0.0.1
 */
@Slf4j
@Component
@RequiredArgsConstructor
@RocketMQMessageListener(
        consumerGroup = MQConstants.GROUP_DATA_ALIGN_NOTE_COMMENT,
        topic = MQConstants.TOPIC_COMMENT_COUNT_CHANGED)
public class TodayNoteCommentIncrementData2DBConsumer implements RocketMQListener<String> {

    /** 日增量表名与布隆 Key 的日期格式（yyyyMMdd） */
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd");

    private final InsertMapper insertMapper;
    private final BloomFilterExecutor bloomFilterExecutor;
    private final TableShardProperties tableShardProperties;

    /**
     * 消费评论总数变更消息，将当日变更的 noteId 去重后落入分片临时表.
     *
     * @param body 消息体 JSON（{@link CommentCountChangedMqDTO}）
     */
    @Override
    public void onMessage(String body) {
        // 1. 反序列化并做基本校验，noteId 缺失直接丢弃
        CommentCountChangedMqDTO dto = JsonUtils.parseObject(body, CommentCountChangedMqDTO.class);
        if (dto == null || dto.getNoteId() == null) {
            return;
        }
        Long noteId = dto.getNoteId();
        // 2. 当日布隆判重：已记录过的 noteId 直接跳过，避免重复落库
        String date = LocalDate.now().format(DATE_FORMATTER);
        String bloomKey = RedisKeyConstants.buildBloomNoteCommentNoteIdKey(date);
        if (bloomFilterExecutor.isAbsent(bloomKey, noteId)) {
            // 3. 首次出现：按 noteId 取模定位分片临时表并落库，再写回布隆
            insertMapper.insertNoteCommentCountTemp(
                    TableConstants.buildTableNameSuffix(date, noteId % tableShardProperties.getShards()), noteId);
            bloomFilterExecutor.add(bloomKey, noteId);
        }
    }
}
