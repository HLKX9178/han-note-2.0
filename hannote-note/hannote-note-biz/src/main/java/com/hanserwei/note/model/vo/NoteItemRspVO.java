package com.hanserwei.note.model.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 个人主页 - 笔记卡片展示数据.
 *
 * @author hanserwei
 * @date 2026/07/18
 * @since 0.0.1
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NoteItemRspVO {

    /** 笔记 ID */
    private Long noteId;

    /** 笔记类型（0：图文 1：视频） */
    private Integer type;

    /** 图文笔记封面（取首图） */
    private String cover;

    /** 视频文件链接 */
    private String videoUri;

    /** 笔记标题 */
    private String title;

    /** 发布者 ID */
    private Long creatorId;

    /** 发布者昵称 */
    private String nickname;

    /** 发布者头像 */
    private String avatar;

    /** 被点赞数（展示字符串，如「13.7万」） */
    private String likeTotal;

    /** 当前登录用户是否已点赞 */
    private Boolean isLiked;
}
