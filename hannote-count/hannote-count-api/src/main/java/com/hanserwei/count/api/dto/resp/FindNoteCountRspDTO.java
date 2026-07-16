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

    /** 笔记 ID */
    private Long noteId;

    /** 点赞总数 */
    private Long likeTotal;

    /** 收藏总数 */
    private Long collectTotal;

    /** 评论总数 */
    private Long commentTotal;
}
