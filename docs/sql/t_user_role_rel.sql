-- hannote 用户角色关联表（PostgreSQL）

CREATE TABLE IF NOT EXISTS t_user_role_rel (
    id           BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    user_id      BIGINT    NOT NULL,
    role_id      BIGINT    NOT NULL,
    create_time  TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time  TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    is_deleted   BOOLEAN   NOT NULL DEFAULT FALSE
);

COMMENT ON TABLE  t_user_role_rel              IS '用户角色关联表';
COMMENT ON COLUMN t_user_role_rel.id           IS '主键 ID';
COMMENT ON COLUMN t_user_role_rel.user_id      IS '用户 ID';
COMMENT ON COLUMN t_user_role_rel.role_id      IS '角色 ID';
COMMENT ON COLUMN t_user_role_rel.create_time  IS '创建时间';
COMMENT ON COLUMN t_user_role_rel.update_time  IS '更新时间';
COMMENT ON COLUMN t_user_role_rel.is_deleted   IS '逻辑删除（false：未删除 true：已删除）';

CREATE INDEX IF NOT EXISTS idx_user_role_rel_user_id ON t_user_role_rel (user_id);
CREATE INDEX IF NOT EXISTS idx_user_role_rel_role_id ON t_user_role_rel (role_id);
