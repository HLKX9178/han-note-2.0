-- hannote 频道-话题关联表（PostgreSQL）
-- 在数据库 hannote 中执行
-- 该表没有 is_deleted 字段：关系记录不做逻辑删除，要解除关系直接物理删除。

CREATE TABLE IF NOT EXISTS t_channel_topic_rel (
    id           BIGINT    NOT NULL PRIMARY KEY,
    channel_id   BIGINT    NOT NULL,
    topic_id     BIGINT    NOT NULL,
    create_time  TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time  TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

COMMENT ON TABLE  t_channel_topic_rel             IS '频道-话题关联表';
COMMENT ON COLUMN t_channel_topic_rel.id          IS '主键 ID（由分布式 ID 服务生成）';
COMMENT ON COLUMN t_channel_topic_rel.channel_id  IS '频道 ID';
COMMENT ON COLUMN t_channel_topic_rel.topic_id    IS '话题 ID';
COMMENT ON COLUMN t_channel_topic_rel.create_time IS '创建时间';
COMMENT ON COLUMN t_channel_topic_rel.update_time IS '更新时间';

CREATE INDEX IF NOT EXISTS idx_channel_topic_rel_channel_id ON t_channel_topic_rel(channel_id);
CREATE INDEX IF NOT EXISTS idx_channel_topic_rel_topic_id   ON t_channel_topic_rel(topic_id);
