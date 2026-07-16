package com.hanserwei.comment.model.vo;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 二级评论分页请求.
 *
 * @author hanserwei
 * @date 2026/07/15
 * @since 0.0.1
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FindChildCommentPageListReqVO {

    /** 一级（父）评论 ID */
    @NotNull(message = "一级评论 ID 不能为空")
    private Long parentCommentId;

    /** 页码，从 1 开始 */
    @Min(value = 1, message = "页码不能小于 1")
    @Builder.Default
    private Integer pageNo = 1;
}
