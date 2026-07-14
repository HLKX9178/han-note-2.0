package com.hanserwei.kv.domain.dataobject;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.cassandra.core.cql.PrimaryKeyType;
import org.springframework.data.cassandra.core.mapping.PrimaryKeyClass;
import org.springframework.data.cassandra.core.mapping.PrimaryKeyColumn;

import java.io.Serializable;
import java.util.UUID;

/**
 * 评论正文表复合主键（映射 ScyllaDB comment_content 主键）.
 *
 * @author hanserwei
 * @date 2026/07/14
 * @since 0.0.1
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@PrimaryKeyClass
public class CommentContentPrimaryKey implements Serializable {

    /** 分区键 1：笔记 ID */
    @PrimaryKeyColumn(name = "note_id", type = PrimaryKeyType.PARTITIONED)
    private Long noteId;

    /** 分区键 2：发布年月 */
    @PrimaryKeyColumn(name = "year_month", type = PrimaryKeyType.PARTITIONED)
    private String yearMonth;

    /** 聚簇列：评论内容 ID */
    @PrimaryKeyColumn(name = "content_id", type = PrimaryKeyType.CLUSTERED)
    private UUID contentId;
}
