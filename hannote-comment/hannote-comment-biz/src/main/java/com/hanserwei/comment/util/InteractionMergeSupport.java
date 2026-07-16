package com.hanserwei.comment.util;

import com.hanserwei.comment.model.dto.LikeUnlikeCommentMqDTO;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 评论互动批次合并工具：同一用户与评论只保留最后操作.
 *
 * @author hanserwei
 * @date 2026/07/15
 * @since 0.0.1
 */
public final class InteractionMergeSupport {

    private InteractionMergeSupport() {
    }

    /**
     * 合并一批点赞/取消点赞消息：同一 (用户, 评论) 只保留最后一次操作.
     *
     * <p>用户短时间内反复点赞/取消会产生多条相互抵消的消息，消费端批量落库前先做折叠，
     * 减少无效 DB 写。先 remove 再 put 以刷新 {@link LinkedHashMap} 中的插入顺序，
     * 使保留下来的消息按其最后一次出现顺序排列；字段不全的脏消息直接丢弃。
     *
     * @param source 原始消息列表，可为 null
     * @return 折叠后的不可变消息列表；入参为 null 返回空列表
     */
    public static List<LikeUnlikeCommentMqDTO> mergeByLastOperation(List<LikeUnlikeCommentMqDTO> source) {
        Map<InteractionKey, LikeUnlikeCommentMqDTO> merged = new LinkedHashMap<>();
        if (source == null) {
            return List.of();
        }
        source.forEach(item -> {
            // 丢弃字段不全的脏消息
            if (item == null || item.getUserId() == null || item.getCommentId() == null || item.getType() == null) {
                return;
            }
            // 先 remove 再 put，刷新顺序并让最后一次操作覆盖前值
            InteractionKey key = new InteractionKey(item.getUserId(), item.getCommentId());
            merged.remove(key);
            merged.put(key, item);
        });
        return List.copyOf(merged.values());
    }

    /**
     * 合并去重键：用户 ID + 评论 ID.
     *
     * @param userId    用户 ID
     * @param commentId 评论 ID
     */
    private record InteractionKey(Long userId, Long commentId) {
    }
}
