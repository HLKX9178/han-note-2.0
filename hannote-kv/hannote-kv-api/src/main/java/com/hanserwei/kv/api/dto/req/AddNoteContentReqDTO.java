package com.hanserwei.kv.api.dto.req;

import jakarta.validation.constraints.NotBlank;
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

    /** 笔记内容 UUID（由笔记服务生成并传入，对应 note_content 表主键） */
    @NotBlank(message = "笔记内容 UUID 不能为空")
    private String uuid;

    /** 笔记内容 */
    @NotBlank(message = "笔记内容不能为空")
    private String content;
}
