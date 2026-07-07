-- hannote 权限表（PostgreSQL）

CREATE TABLE IF NOT EXISTS t_permission (
    id              BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    parent_id       BIGINT       NOT NULL DEFAULT 0,
    name            VARCHAR(16)  NOT NULL,
    type            SMALLINT     NOT NULL,
    menu_url        VARCHAR(32)  NOT NULL DEFAULT '',
    menu_icon       VARCHAR(255) NOT NULL DEFAULT '',
    sort            INT          NOT NULL DEFAULT 0,
    permission_key  VARCHAR(64)  NOT NULL,
    status          SMALLINT     NOT NULL DEFAULT 0,
    create_time     TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time     TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    is_deleted      BOOLEAN      NOT NULL DEFAULT FALSE
);

COMMENT ON TABLE  t_permission                  IS '权限表';
COMMENT ON COLUMN t_permission.id               IS '主键 ID';
COMMENT ON COLUMN t_permission.parent_id        IS '父 ID（用于构建权限树）';
COMMENT ON COLUMN t_permission.name             IS '权限名称';
COMMENT ON COLUMN t_permission.type             IS '类型（1：目录 2：菜单 3：按钮）';
COMMENT ON COLUMN t_permission.menu_url         IS '菜单路由';
COMMENT ON COLUMN t_permission.menu_icon        IS '菜单图标';
COMMENT ON COLUMN t_permission.sort             IS '管理系统中的显示顺序';
COMMENT ON COLUMN t_permission.permission_key   IS '权限唯一标识（供鉴权框架使用）';
COMMENT ON COLUMN t_permission.status           IS '状态（0：启用 1：禁用）';
COMMENT ON COLUMN t_permission.create_time      IS '创建时间';
COMMENT ON COLUMN t_permission.update_time      IS '更新时间';
COMMENT ON COLUMN t_permission.is_deleted       IS '逻辑删除（false：未删除 true：已删除）';

CREATE UNIQUE INDEX IF NOT EXISTS uk_permission_key ON t_permission (permission_key);
