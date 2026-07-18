package com.hanserwei.note.model.vo;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 个人主页 - 已发布笔记列表请求.
 *
 * @author hanserwei
 * @date 2026/07/18
 * @since 0.0.1
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FindPublishedNoteListReqVO {

    /** 目标博主的用户 ID */
    @NotNull(message = "用户 ID 不能为空")
    private Long userId;

    /** 游标（即笔记 ID，用于分页）；为空表示查询第一页 */
    private Long cursor;
}
