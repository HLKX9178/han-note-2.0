package com.hanserwei.kv.consumer;

import com.hanserwei.framework.common.util.JsonUtils;
import com.hanserwei.kv.api.dto.req.AddNoteContentReqDTO;
import com.hanserwei.kv.constant.MQConstants;
import com.hanserwei.kv.model.dto.PublishNoteDTO;
import com.hanserwei.kv.service.NoteContentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.stereotype.Component;

/**
 * 发布笔记事务消息消费者：把笔记正文写入 ScyllaDB.
 *
 * <p>笔记服务本地事务（写 t_note 元数据）COMMIT 后，本消费者才可见并消费该消息，
 * 按 {@code contentUuid} 将正文落库到 ScyllaDB（幂等：同 UUID 覆盖写）。
 *
 * @author hanserwei
 * @date 2026/07/18
 * @since 0.0.1
 */
@Slf4j
@Component
@RequiredArgsConstructor
@RocketMQMessageListener(
        topic = MQConstants.TOPIC_PUBLISH_NOTE_TRANSACTION,
        consumerGroup = MQConstants.GROUP_SAVE_NOTE_CONTENT)
public class SaveNoteContentConsumer implements RocketMQListener<String> {

    private final NoteContentService noteContentService;

    @Override
    public void onMessage(String body) {
        log.info("==> KV 消费发布笔记事务消息: {}", body);
        if (StringUtils.isBlank(body)) {
            return;
        }
        PublishNoteDTO publishNoteDTO = JsonUtils.parseObject(body, PublishNoteDTO.class);
        if (publishNoteDTO == null || StringUtils.isBlank(publishNoteDTO.getContentUuid())) {
            log.warn("==> KV 发布笔记事务消息缺少 contentUuid，跳过, body: {}", body);
            return;
        }

        noteContentService.addNoteContent(AddNoteContentReqDTO.builder()
                .uuid(publishNoteDTO.getContentUuid())
                .content(publishNoteDTO.getContent())
                .build());
    }
}
