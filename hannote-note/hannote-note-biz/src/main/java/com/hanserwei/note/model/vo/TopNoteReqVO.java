package com.hanserwei.note.model.vo;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 笔记置顶 / 取消置顶请求.
 *
 * @author hanserwei
 * @date 2026/07/09
 * @since 0.0.1
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TopNoteReqVO {

    /** 笔记 ID */
    @NotNull(message = "笔记 ID 不能为空")
    private Long id;

    /** 是否置顶（true：置顶 false：取消置顶） */
    @NotNull(message = "置顶状态不能为空")
    private Boolean isTop;
}
