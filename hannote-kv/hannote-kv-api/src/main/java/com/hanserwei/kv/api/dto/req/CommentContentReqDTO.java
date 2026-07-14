package com.hanserwei.kv.api.dto.req;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 评论内容（服务间调用，批量新增的单元素）.
 *
 * @author hanserwei
 * @date 2026/07/14
 * @since 0.0.1
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CommentContentReqDTO {

    /** 笔记 ID（复合分区键 1） */
    @NotNull(message = "笔记 ID 不能为空")
    private Long noteId;

    /** 发布年月，格式 yyyy-MM（复合分区键 2） */
    @NotBlank(message = "发布年月不能为空")
    private String yearMonth;

    /** 评论内容 UUID（聚簇列） */
    @NotBlank(message = "评论正文 ID 不能为空")
    private String contentId;

    /** 评论正文 */
    @NotBlank(message = "评论正文不能为空")
    private String content;
}
