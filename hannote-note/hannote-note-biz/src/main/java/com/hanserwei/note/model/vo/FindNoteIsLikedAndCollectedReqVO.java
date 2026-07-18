package com.hanserwei.note.model.vo;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 获取当前登录用户对某笔记「是否点赞、是否收藏」的入参.
 *
 * @author hanserwei
 * @date 2026/07/17
 * @since 0.0.1
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FindNoteIsLikedAndCollectedReqVO {

    /** 笔记 ID */
    @NotNull(message = "笔记 ID 不能为空")
    private Long noteId;
}
