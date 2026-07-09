package com.hanserwei.note.model.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 笔记详情响应.
 *
 * @author hanserwei
 * @date 2026/07/09
 * @since 0.0.1
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FindNoteDetailRspVO {

    /** 笔记 ID */
    private Long id;

    /** 笔记类型（0：图文 1：视频） */
    private Integer type;

    /** 标题 */
    private String title;

    /** 正文内容 */
    private String content;

    /** 图片链接集合（图文笔记） */
    private List<String> imgUris;

    /** 话题 ID */
    private Long topicId;

    /** 话题名称 */
    private String topicName;

    /** 发布者用户 ID */
    private Long creatorId;

    /** 发布者昵称 */
    private String creatorName;

    /** 发布者头像 */
    private String avatar;

    /** 视频链接（视频笔记） */
    private String videoUri;

    /** 编辑时间 */
    private LocalDateTime updateTime;

    /** 可见范围（0：公开 1：仅自己可见） */
    private Integer visible;
}
