package com.hanserwei.kv.api.dto.resp;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * 查询笔记内容响应（服务间调用）.
 *
 * @author hanserwei
 * @date 2026/07/08
 * @since 0.0.1
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FindNoteContentRspDTO {

    /** 笔记 ID */
    private UUID noteId;

    /** 笔记内容 */
    private String content;
}
