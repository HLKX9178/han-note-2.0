package com.hanserwei.note.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 发布笔记事务消息体.
 *
 * <p>除笔记元数据（与 {@code NoteDO} 一致）外，额外携带 {@code content} 正文，
 * 供 KV 服务消费端把正文写入 ScyllaDB。
 *
 * @author hanserwei
 * @date 2026/07/18
 * @since 0.0.1
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PublishNoteDTO {

    /** 笔记 ID */
    private Long id;

    /** 标题 */
    private String title;

    /** 正文是否为空 */
    private Boolean contentEmpty;

    /** 发布者 ID */
    private Long creatorId;

    /** 话题 ID */
    private Long topicId;

    /** 话题名称 */
    private String topicName;

    /** 是否置顶 */
    private Boolean top;

    /** 类型（0：图文 1：视频） */
    private Integer type;

    /** 图片链接（逗号分隔） */
    private String imgUris;

    /** 视频链接 */
    private String videoUri;

    /** 可见范围（0：公开 1：仅自己可见） */
    private Integer visible;

    /** 状态（1：正常展示） */
    private Integer status;

    /** 创建时间 */
    private LocalDateTime createTime;

    /** 更新时间 */
    private LocalDateTime updateTime;

    /** 笔记内容 UUID（关联 ScyllaDB note_content.id） */
    private String contentUuid;

    /** 笔记正文（仅事务消息传输用，不落 t_note） */
    private String content;
}
