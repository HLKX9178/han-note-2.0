package com.hanserwei.kv.domain.dataobject;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.cassandra.core.mapping.PrimaryKey;
import org.springframework.data.cassandra.core.mapping.Table;

import java.util.UUID;

/**
 * 笔记内容数据对象（映射 ScyllaDB 表 {@code note_content}）.
 *
 * @author hanserwei
 * @date 2026/07/08
 * @since 0.0.1
 */
@Table("note_content")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NoteContentDO {

    /** 主键 */
    @PrimaryKey("id")
    private UUID id;

    /** 笔记正文内容 */
    private String content;
}
