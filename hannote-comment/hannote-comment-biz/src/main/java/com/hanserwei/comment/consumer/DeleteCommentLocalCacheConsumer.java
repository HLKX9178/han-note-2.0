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
 * <p>消费 {@code DeleteCommentLocalCacheTopic}，采用 {@link MessageModel#BROADCASTING} 广播模式，
 * 使集群内每个实例都收到并失效各自的 L1 本地详情缓存，保证多实例缓存最终一致。
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

    /** 评论缓存管理器：失效本机 L1 详情缓存 */
    private final CommentCacheManager commentCacheManager;

    /**
     * 消费广播消息：失效本实例中对应评论 ID 的 L1 详情缓存.
     *
     * @param body 消息体 JSON（{@link DeleteCommentLocalCacheMqDTO}）
     */
    @Override
    public void onMessage(String body) {
        DeleteCommentLocalCacheMqDTO message = JsonUtils.parseObject(body, DeleteCommentLocalCacheMqDTO.class);
        if (message != null && message.getCommentIds() != null) {
            commentCacheManager.invalidateLocalDetails(message.getCommentIds());
        }
    }
}
