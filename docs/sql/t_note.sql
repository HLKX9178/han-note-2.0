-- hannote 笔记表（PostgreSQL）
-- 在数据库 hannote 中执行

CREATE TABLE IF NOT EXISTS t_note (
    id                BIGINT        NOT NULL PRIMARY KEY,
    title             VARCHAR(64)   NOT NULL,
    is_content_empty  BOOLEAN       NOT NULL DEFAULT FALSE,
    creator_id        BIGINT        NOT NULL,
    topic_id          BIGINT,
    topic_name        VARCHAR(32)   DEFAULT '',
    is_top            BOOLEAN       NOT NULL DEFAULT FALSE,
    type              SMALLINT      DEFAULT 0,
    img_uris          VARCHAR(660),
    video_uri         VARCHAR(120),
    visible           SMALLINT      DEFAULT 0,
    status            SMALLINT      NOT NULL DEFAULT 0,
    create_time       TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time       TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP
);

COMMENT ON TABLE  t_note                    IS '笔记表';
COMMENT ON COLUMN t_note.id                 IS '主键 ID（由分布式 ID 服务生成）';
COMMENT ON COLUMN t_note.title              IS '标题';
COMMENT ON COLUMN t_note.is_content_empty   IS '正文是否为空（false：不为空 true：空）';
COMMENT ON COLUMN t_note.creator_id         IS '发布者 ID';
COMMENT ON COLUMN t_note.topic_id           IS '话题 ID';
COMMENT ON COLUMN t_note.topic_name         IS '话题名称（冗余字段）';
COMMENT ON COLUMN t_note.is_top             IS '是否置顶（false：未置顶 true：置顶）';
COMMENT ON COLUMN t_note.type               IS '类型（0：图文 1：视频）';
COMMENT ON COLUMN t_note.img_uris           IS '笔记图片链接（逗号隔开，最多 9 张）';
COMMENT ON COLUMN t_note.video_uri          IS '视频链接';
COMMENT ON COLUMN t_note.visible            IS '可见范围（0：公开 1：仅自己可见）';
COMMENT ON COLUMN t_note.status             IS '状态（0：待审核 1：正常展示 2：被删除 3：被下架）';
COMMENT ON COLUMN t_note.create_time        IS '创建时间';
COMMENT ON COLUMN t_note.update_time        IS '更新时间';

-- 笔记内容 UUID（关联 ScyllaDB note_content.id）；追加
ALTER TABLE t_note ADD COLUMN IF NOT EXISTS content_uuid VARCHAR(36) NOT NULL DEFAULT '';
COMMENT ON COLUMN t_note.content_uuid IS '笔记内容 UUID（关联 ScyllaDB note_content.id）';

-- 移除逻辑删除列：笔记删除改用 status=2 表示（决策，见 PRD 2026-07-09-note-rocketmq-and-cache-consistency）
ALTER TABLE t_note DROP COLUMN IF EXISTS is_deleted;

CREATE INDEX IF NOT EXISTS idx_note_creator_id ON t_note(creator_id);
CREATE INDEX IF NOT EXISTS idx_note_topic_id   ON t_note(topic_id);
CREATE INDEX IF NOT EXISTS idx_note_status     ON t_note(status);
