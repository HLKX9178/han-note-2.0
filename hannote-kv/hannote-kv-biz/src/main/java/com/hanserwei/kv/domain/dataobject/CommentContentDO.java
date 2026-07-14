package com.hanserwei.kv.domain.dataobject;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.cassandra.core.mapping.PrimaryKey;
import org.springframework.data.cassandra.core.mapping.Table;

/**
 * 评论正文数据对象（映射 ScyllaDB 表 comment_content）.
 *
 * @author hanserwei
 * @date 2026/07/14
 * @since 0.0.1
 */
@Table("comment_content")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CommentContentDO {

    /** 复合主键 */
    @PrimaryKey
    private CommentContentPrimaryKey primaryKey;

    /** 评论正文 */
    private String content;
}
