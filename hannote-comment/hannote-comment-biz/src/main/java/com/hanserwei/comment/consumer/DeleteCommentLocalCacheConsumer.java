package com.hanserwei.comment.consumer;

import com.hanserwei.comment.cache.CommentCacheManager;
import com.hanserwei.comment.constant.MQConstants;
import com.hanserwei.comment.model.dto.DeleteCommentLocalCacheMqDTO;
import com.hanserwei.framework.common.util.JsonUtils;
import lombok.RequiredArgsConstructor;
import org.apache.rocketmq.spring.annotation.MessageModel;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.stereotype.Component;

/**
 * 广播删除各评论服务实例的 Caffeine 详情缓存.
 *
 * @author hanserwei
 * @date 2026/07/15
 * @since 0.0.1
 */
@Component
@RequiredArgsConstructor
@RocketMQMessageListener(
        consumerGroup = MQConstants.GROUP_DELETE_LOCAL_CACHE,
        topic = MQConstants.TOPIC_DELETE_COMMENT_LOCAL_CACHE,
        messageModel = MessageModel.BROADCASTING)
public class DeleteCommentLocalCacheConsumer implements RocketMQListener<String> {

    private final CommentCacheManager commentCacheManager;

    @Override
    public void onMessage(String body) {
        DeleteCommentLocalCacheMqDTO message = JsonUtils.parseObject(body, DeleteCommentLocalCacheMqDTO.class);
        if (message != null && message.getCommentIds() != null) {
            commentCacheManager.invalidateLocalDetails(message.getCommentIds());
        }
    }
}
