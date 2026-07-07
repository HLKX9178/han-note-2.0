-- hannote 角色表（PostgreSQL）

CREATE TABLE IF NOT EXISTS t_role (
    id           BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    role_name    VARCHAR(32)  NOT NULL,
    role_key     VARCHAR(32)  NOT NULL,
    status       SMALLINT     NOT NULL DEFAULT 0,
    sort         INT          NOT NULL DEFAULT 0,
    remark       VARCHAR(255),
    create_time  TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time  TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    is_deleted   BOOLEAN      NOT NULL DEFAULT FALSE
);

COMMENT ON TABLE  t_role              IS '角色表';
COMMENT ON COLUMN t_role.id           IS '主键 ID';
COMMENT ON COLUMN t_role.role_name    IS '角色名';
COMMENT ON COLUMN t_role.role_key     IS '角色唯一标识';
COMMENT ON COLUMN t_role.status       IS '状态（0：启用 1：禁用）';
COMMENT ON COLUMN t_role.sort         IS '管理系统中的显示顺序';
COMMENT ON COLUMN t_role.remark       IS '备注';
COMMENT ON COLUMN t_role.create_time  IS '创建时间';
COMMENT ON COLUMN t_role.update_time  IS '最后一次更新时间';
COMMENT ON COLUMN t_role.is_deleted   IS '逻辑删除（false：未删除 true：已删除）';

CREATE UNIQUE INDEX IF NOT EXISTS uk_role_role_key ON t_role (role_key);
