-- hannote 评论表（PostgreSQL）
-- 在数据库 hannote 中执行
-- 属主：hannote-comment 服务（CommentDO）
-- 主键 id 由 CoSId 预生成写入（非自增）
CREATE TABLE IF NOT EXISTS t_comment (
    id                BIGINT       NOT NULL PRIMARY KEY,
    note_id           BIGINT       NOT NULL,
    user_id           BIGINT       NOT NULL,
    content_uuid      VARCHAR(36)  DEFAULT '',
    is_content_empty  BOOLEAN      NOT NULL DEFAULT FALSE,
    image_url         VARCHAR(120) NOT NULL DEFAULT '',
    level             SMALLINT     NOT NULL DEFAULT 1,
    reply_total       BIGINT       DEFAULT 0,
    like_total        BIGINT       DEFAULT 0,
    parent_id         BIGINT       DEFAULT 0,
    reply_comment_id  BIGINT       DEFAULT 0,
    reply_user_id     BIGINT       DEFAULT 0,
    is_top            BOOLEAN      NOT NULL DEFAULT FALSE,
    create_time       TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time       TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_comment_note_id          ON t_comment(note_id);
CREATE INDEX IF NOT EXISTS idx_comment_user_id          ON t_comment(user_id);
CREATE INDEX IF NOT EXISTS idx_comment_parent_id        ON t_comment(parent_id);
CREATE INDEX IF NOT EXISTS idx_comment_create_time      ON t_comment(create_time);
CREATE INDEX IF NOT EXISTS idx_comment_reply_comment_id ON t_comment(reply_comment_id);
CREATE INDEX IF NOT EXISTS idx_comment_reply_user_id    ON t_comment(reply_user_id);

COMMENT ON TABLE  t_comment                  IS '评论表';
COMMENT ON COLUMN t_comment.id               IS '评论 ID（CoSId 预生成）';
COMMENT ON COLUMN t_comment.note_id          IS '关联的笔记 ID';
COMMENT ON COLUMN t_comment.user_id          IS '发布者用户 ID';
COMMENT ON COLUMN t_comment.content_uuid     IS '评论内容 UUID（关联 ScyllaDB comment_content）';
COMMENT ON COLUMN t_comment.is_content_empty IS '内容是否为空（仅图片时为 true）';
COMMENT ON COLUMN t_comment.image_url        IS '评论附加图片 URL';
COMMENT ON COLUMN t_comment.level            IS '级别（1 一级 2 二级）';
COMMENT ON COLUMN t_comment.reply_total      IS '被回复次数（仅一级评论）';
COMMENT ON COLUMN t_comment.like_total       IS '被点赞次数';
COMMENT ON COLUMN t_comment.parent_id        IS '父 ID（对笔记评论=笔记ID；二级=一级评论ID）';
COMMENT ON COLUMN t_comment.reply_comment_id IS '回复的评论 ID（0=对笔记评论）';
COMMENT ON COLUMN t_comment.reply_user_id    IS '回复的用户 ID';
COMMENT ON COLUMN t_comment.is_top           IS '是否置顶';
