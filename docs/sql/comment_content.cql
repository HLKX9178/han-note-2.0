-- hannote 评论正文表（ScyllaDB，keyspace: hannote）
-- 属主：hannote-kv 服务（CommentContentDO）
-- 复合分区键 (note_id, year_month) 分散热点；聚簇列 content_id 保证唯一 + 天然幂等。
CREATE TABLE IF NOT EXISTS comment_content (
    note_id     BIGINT,
    year_month  TEXT,
    content_id  UUID,
    content     TEXT,
    PRIMARY KEY ((note_id, year_month), content_id)
);
