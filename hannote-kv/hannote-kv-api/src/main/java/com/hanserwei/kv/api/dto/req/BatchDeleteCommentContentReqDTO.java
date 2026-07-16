package com.hanserwei.kv.api.dto.req;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 批量删除评论正文请求.
 *
 * @author hanserwei
 * @date 2026/07/15
 * @since 0.0.1
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BatchDeleteCommentContentReqDTO {

    /** 笔记 ID（ScyllaDB 分区键的一部分，同一批删除须归属同一笔记） */
    @NotNull(message = "笔记 ID 不能为空")
    private Long noteId;

    /** 待删除的评论正文主键集合（年月 + 正文 UUID），单次上限 50 条 */
    @Valid
    @NotEmpty(message = "评论正文主键集合不能为空")
    @Size(max = 50, message = "单次最多删除 50 条评论正文")
    private List<CommentContentKeyReqDTO> items;
}
