-- hannote 频道表（PostgreSQL）
-- 在数据库 hannote 中执行

CREATE TABLE IF NOT EXISTS t_channel (
    id           BIGINT       NOT NULL PRIMARY KEY,
    name         VARCHAR(12)  NOT NULL,
    create_time  TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time  TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    is_deleted   BOOLEAN      NOT NULL DEFAULT FALSE
);

COMMENT ON TABLE  t_channel             IS '频道表';
COMMENT ON COLUMN t_channel.id          IS '主键 ID（由分布式 ID 服务生成）';
COMMENT ON COLUMN t_channel.name        IS '频道名称';
COMMENT ON COLUMN t_channel.create_time IS '创建时间';
COMMENT ON COLUMN t_channel.update_time IS '更新时间';
COMMENT ON COLUMN t_channel.is_deleted  IS '逻辑删除（false：未删除 true：已删除）';
