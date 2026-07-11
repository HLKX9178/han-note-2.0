package com.hanserwei.note.model.vo;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 收藏笔记请求.
 *
 * @author hanserwei
 * @date 2026/07/11
 * @since 0.0.1
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CollectNoteReqVO {

    /** 笔记 ID */
    @NotNull(message = "笔记 ID 不能为空")
    private Long id;
}
