package com.hanserwei.note.model.vo;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 笔记更新请求.
 *
 * @author hanserwei
 * @date 2026/07/09
 * @since 0.0.1
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateNoteReqVO {

    /** 笔记 ID */
    @NotNull(message = "笔记 ID 不能为空")
    private Long id;

    /** 笔记类型（0：图文 1：视频） */
    @NotNull(message = "笔记类型不能为空")
    private Integer type;

    /** 图片链接集合（图文笔记必填，最多 8 张） */
    private List<String> imgUris;

    /** 视频链接（视频笔记必填） */
    private String videoUri;

    /** 笔记标题 */
    private String title;

    /** 笔记正文（可空） */
    private String content;

    /** 话题 ID（可空） */
    private Long topicId;
}
