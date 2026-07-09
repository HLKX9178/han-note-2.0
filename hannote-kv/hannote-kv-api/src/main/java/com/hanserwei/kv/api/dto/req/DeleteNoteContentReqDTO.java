package com.hanserwei.kv.api.dto.req;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 删除笔记内容请求（服务间调用）.
 *
 * @author hanserwei
 * @date 2026/07/08
 * @since 0.0.1
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DeleteNoteContentReqDTO {

    /** 笔记内容 UUID（字符串形式） */
    @NotBlank(message = "笔记内容 UUID 不能为空")
    private String uuid;
}
