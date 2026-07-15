package com.hanserwei.count.api.dto.resp;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 笔记维度计数响应.
 *
 * @author hanserwei
 * @date 2026/07/15
 * @since 0.0.1
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FindNoteCountRspDTO {

    private Long noteId;
    private Long likeTotal;
    private Long collectTotal;
    private Long commentTotal;
}
