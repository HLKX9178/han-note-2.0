package com.hanserwei.comment.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 批量删除评论正文消息.
 *
 * @author hanserwei
 * @date 2026/07/15
 * @since 0.0.1
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DeleteCommentContentMqDTO {
    /** 关联的笔记 ID */
    private Long noteId;
    /** 待删除正文的复合主键列表 */
    private List<DeleteCommentContentItemMqDTO> items;
}
