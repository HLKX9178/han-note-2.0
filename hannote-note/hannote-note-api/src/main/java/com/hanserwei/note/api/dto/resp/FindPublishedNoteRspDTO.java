package com.hanserwei.note.api.dto.resp;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 已发布笔记最小信息响应.
 *
 * @author hanserwei
 * @date 2026/07/15
 * @since 0.0.1
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FindPublishedNoteRspDTO {

    /** 笔记 ID */
    private Long noteId;
    /** 笔记发布者用户 ID */
    private Long creatorId;
}
