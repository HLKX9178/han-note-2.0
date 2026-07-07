-- hannote 用户表（PostgreSQL）
-- 在数据库 hannote 中执行

CREATE TABLE IF NOT EXISTS t_user (
    id               BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    hannote_id       VARCHAR(15)  NOT NULL,
    password         VARCHAR(128),
    nickname         VARCHAR(24)  NOT NULL,
    avatar           VARCHAR(120),
    birthday         DATE,
    background_img   VARCHAR(120),
    phone            VARCHAR(11)  NOT NULL,
    sex              SMALLINT     DEFAULT 0,
    status           SMALLINT     NOT NULL DEFAULT 0,
    introduction     VARCHAR(100),
    create_time      TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time      TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    is_deleted       BOOLEAN      NOT NULL DEFAULT FALSE
);

COMMENT ON TABLE  t_user                   IS '用户表';
COMMENT ON COLUMN t_user.id                IS '主键 ID';
COMMENT ON COLUMN t_user.hannote_id        IS '小憨书号（唯一凭证，系统生成）';
COMMENT ON COLUMN t_user.password          IS '密码（BCrypt 加密后存储）';
COMMENT ON COLUMN t_user.nickname          IS '昵称（默认：小憨薯 + hannoteId）';
COMMENT ON COLUMN t_user.avatar            IS '头像 URL';
COMMENT ON COLUMN t_user.birthday          IS '生日';
COMMENT ON COLUMN t_user.background_img    IS '背景图 URL';
COMMENT ON COLUMN t_user.phone             IS '手机号';
COMMENT ON COLUMN t_user.sex               IS '性别（0：女 1：男）';
COMMENT ON COLUMN t_user.status            IS '状态（0：启用 1：禁用）';
COMMENT ON COLUMN t_user.introduction      IS '个人简介';
COMMENT ON COLUMN t_user.create_time       IS '创建时间';
COMMENT ON COLUMN t_user.update_time       IS '更新时间';
COMMENT ON COLUMN t_user.is_deleted        IS '逻辑删除（false：未删除 true：已删除）';

CREATE UNIQUE INDEX IF NOT EXISTS uk_user_hannote_id ON t_user (hannote_id);
CREATE UNIQUE INDEX IF NOT EXISTS uk_user_phone      ON t_user (phone);
