package com.hanserwei.kv.api.dto.req;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 新增笔记内容请求（服务间调用）.
 *
 * @author hanserwei
 * @date 2026/07/08
 * @since 0.0.1
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AddNoteContentReqDTO {

    /** 笔记 ID（由笔记服务生成并传入） */
    @NotNull(message = "笔记 ID 不能为空")
    private Long noteId;

    /** 笔记内容 */
    @NotBlank(message = "笔记内容不能为空")
    private String content;
}
