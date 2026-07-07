-- hannote 角色权限关联表（PostgreSQL）

CREATE TABLE IF NOT EXISTS t_role_permission_rel (
    id             BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    role_id        BIGINT    NOT NULL,
    permission_id  BIGINT    NOT NULL,
    create_time    TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time    TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    is_deleted     BOOLEAN   NOT NULL DEFAULT FALSE
);

COMMENT ON TABLE  t_role_permission_rel                IS '角色权限关联表';
COMMENT ON COLUMN t_role_permission_rel.id             IS '主键 ID';
COMMENT ON COLUMN t_role_permission_rel.role_id        IS '角色 ID';
COMMENT ON COLUMN t_role_permission_rel.permission_id  IS '权限 ID';
COMMENT ON COLUMN t_role_permission_rel.create_time    IS '创建时间';
COMMENT ON COLUMN t_role_permission_rel.update_time    IS '更新时间';
COMMENT ON COLUMN t_role_permission_rel.is_deleted     IS '逻辑删除（false：未删除 true：已删除）';

CREATE INDEX IF NOT EXISTS idx_role_perm_rel_role_id       ON t_role_permission_rel (role_id);
CREATE INDEX IF NOT EXISTS idx_role_perm_rel_permission_id ON t_role_permission_rel (permission_id);
