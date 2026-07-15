package com.hanserwei.comment.consumer;

import com.hanserwei.comment.constant.MQConstants;
import com.hanserwei.comment.model.dto.DeleteCommentContentMqDTO;
import com.hanserwei.comment.rpc.KeyValueRpcService;
import com.hanserwei.framework.common.util.JsonUtils;
import lombok.RequiredArgsConstructor;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.stereotype.Component;

/**
 * 评论正文删除消费者，RPC 调 KV 批量物理删除 ScyllaDB 数据.
 *
 * @author hanserwei
 * @date 2026/07/15
 * @since 0.0.1
 */
@Component
@RequiredArgsConstructor
@RocketMQMessageListener(
        consumerGroup = MQConstants.GROUP_DELETE_CONTENT,
        topic = MQConstants.TOPIC_DELETE_COMMENT_CONTENT)
public class DeleteCommentContentConsumer implements RocketMQListener<String> {

    private final KeyValueRpcService keyValueRpcService;

    @Override
    public void onMessage(String body) {
        DeleteCommentContentMqDTO message = JsonUtils.parseObject(body, DeleteCommentContentMqDTO.class);
        if (message != null && message.getItems() != null && !message.getItems().isEmpty()) {
            keyValueRpcService.batchDeleteCommentContent(message);
        }
    }
}
