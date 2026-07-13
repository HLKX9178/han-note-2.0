package com.hanserwei.search.model.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 搜索笔记返参.
 *
 * @author hanserwei
 * @date 2026/07/13
 * @since 0.0.1
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SearchNoteRspVO {

    /** 笔记 ID */
    private Long noteId;

    /** 封面图 */
    private String cover;

    /** 标题 */
    private String title;

    /** 标题：关键词高亮（{@code <em>} 包裹） */
    private String highlightTitle;

    /** 发布者头像（索引 creator_avatar） */
    private String avatar;

    /** 发布者昵称（索引 creator_nickname） */
    private String nickname;

    /** 最后更新时间（相对时间展示，如「3小时前」「昨天 20:12」） */
    private String updateTime;

    /** 被点赞数（格式化展示） */
    private String likeTotal;

    /** 被评论数（格式化展示） */
    private String commentTotal;

    /** 被收藏数（格式化展示） */
    private String collectTotal;
}
