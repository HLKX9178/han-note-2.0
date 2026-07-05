-- hannote 示例表 t_user（PostgreSQL）
-- 在数据库 hannote 中执行

CREATE TABLE IF NOT EXISTS t_user (
    id          BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    username    VARCHAR(32) NOT NULL,
    create_time TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP
);

COMMENT ON TABLE  t_user             IS '用户测试表';
COMMENT ON COLUMN t_user.id          IS '主键id';
COMMENT ON COLUMN t_user.username    IS '用户名';
COMMENT ON COLUMN t_user.create_time IS '创建时间';
COMMENT ON COLUMN t_user.update_time IS '更新时间';
