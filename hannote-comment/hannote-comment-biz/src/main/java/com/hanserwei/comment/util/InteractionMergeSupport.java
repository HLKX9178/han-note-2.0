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

    public static List<LikeUnlikeCommentMqDTO> mergeByLastOperation(List<LikeUnlikeCommentMqDTO> source) {
        Map<InteractionKey, LikeUnlikeCommentMqDTO> merged = new LinkedHashMap<>();
        if (source == null) {
            return List.of();
        }
        source.forEach(item -> {
            if (item == null || item.getUserId() == null || item.getCommentId() == null || item.getType() == null) {
                return;
            }
            InteractionKey key = new InteractionKey(item.getUserId(), item.getCommentId());
            merged.remove(key);
            merged.put(key, item);
        });
        return List.copyOf(merged.values());
    }

    private record InteractionKey(Long userId, Long commentId) {
    }
}
