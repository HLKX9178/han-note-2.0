package com.hanserwei.comment.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 待删除评论正文复合主键项.
 *
 * @author hanserwei
 * @date 2026/07/15
 * @since 0.0.1
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DeleteCommentContentItemMqDTO {
    /** 分表年月（ScyllaDB comment_content 分区键） */
    private String yearMonth;
    /** 评论正文 ID（即 contentUuid） */
    private String contentId;
}
