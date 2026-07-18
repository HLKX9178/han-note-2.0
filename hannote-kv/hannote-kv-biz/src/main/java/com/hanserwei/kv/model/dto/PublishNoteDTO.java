package com.hanserwei.kv.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 发布笔记事务消息体（KV 侧最小视图）.
 *
 * <p>与笔记服务的消息体同 Topic，本服务仅关心正文 {@code content} 与内容 UUID {@code contentUuid}，
 * 其余元数据字段反序列化时忽略（{@code JsonUtils} 已配置忽略未知字段）。
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

    /** 笔记内容 UUID（ScyllaDB note_content 主键） */
    private String contentUuid;

    /** 笔记正文 */
    private String content;
}
