package com.hanserwei.note.domain.dataobject;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 笔记表数据对象.
 *
 * <p>对应数据表 {@code t_note}。笔记服务核心业务表，记录笔记元信息
 * （标题、作者、话题、类型、媒体链接、可见性、状态等）；笔记正文
 * 存储于 KV 服务 {@code hannote-kv} 的 {@code note_content} 表。
 *
 * @author hanserwei
 * @date 2026/07/09
 * @since 0.0.1
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("t_note")
public class NoteDO {

    /** 主键 ID */
    @TableId(type = IdType.INPUT)
    private Long id;

    /** 标题 */
    private String title;

    /** 正文是否为空（false：不为空 true：空）。为空时无需调 KV 服务取正文 */
    @TableField("is_content_empty")
    private Boolean contentEmpty;

    /** 发布者 ID */
    private Long creatorId;

    /** 话题 ID */
    private Long topicId;

    /** 话题名称（冗余字段，避免反查 t_topic） */
    private String topicName;

    /** 是否置顶（false：未置顶 true：置顶），对应列 is_top */
    @TableField("is_top")
    private Boolean top;

    /** 类型（0：图文 1：视频） */
    private Integer type;

    /** 笔记图片链接（逗号隔开，最多 9 张） */
    private String imgUris;

    /** 视频链接 */
    private String videoUri;

    /** 可见范围（0：公开 1：仅自己可见） */
    private Integer visible;

    /** 状态（0：待审核 1：正常展示 2：被删除 3：被下架） */
    private Integer status;

    /** 创建时间 */
    private LocalDateTime createTime;

    /** 更新时间 */
    private LocalDateTime updateTime;

    /** 笔记内容 UUID（关联 ScyllaDB note_content.id；正文为空时为空串） */
    private String contentUuid;
}
