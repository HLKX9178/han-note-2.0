package com.hanserwei.note.api.dto.req;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 查询已发布笔记请求.
 *
 * @author hanserwei
 * @date 2026/07/15
 * @since 0.0.1
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FindPublishedNoteReqDTO {

    @NotNull(message = "笔记 ID 不能为空")
    private Long noteId;
}
