package com.hanserwei.comment.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 广播删除评论本地缓存消息.
 *
 * @author hanserwei
 * @date 2026/07/15
 * @since 0.0.1
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DeleteCommentLocalCacheMqDTO {
    /** 待各节点本地缓存清除的评论 ID 列表 */
    private List<Long> commentIds;
}
