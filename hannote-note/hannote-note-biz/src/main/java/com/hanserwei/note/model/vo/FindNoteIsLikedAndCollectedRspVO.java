package com.hanserwei.note.model.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 获取「是否点赞、是否收藏」的返参.
 *
 * @author hanserwei
 * @date 2026/07/17
 * @since 0.0.1
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FindNoteIsLikedAndCollectedRspVO {

    /** 笔记 ID */
    private Long noteId;

    /** 当前登录用户是否已点赞 */
    private Boolean isLiked;

    /** 当前登录用户是否已收藏 */
    private Boolean isCollected;
}
