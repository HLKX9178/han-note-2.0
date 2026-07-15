-- 评论服务二期增量迁移（PostgreSQL）
-- 属主：hannote-comment；执行前请先备份并在测试环境验证。

ALTER TABLE t_comment
    ADD COLUMN IF NOT EXISTS first_reply_comment_id BIGINT NOT NULL DEFAULT 0;

ALTER TABLE t_comment
    ADD COLUMN IF NOT EXISTS heat NUMERIC(20, 2) NOT NULL DEFAULT 0.00;

UPDATE t_comment SET reply_total = 0 WHERE reply_total IS NULL;
UPDATE t_comment SET like_total = 0 WHERE like_total IS NULL;

ALTER TABLE t_comment ALTER COLUMN reply_total SET DEFAULT 0;
ALTER TABLE t_comment ALTER COLUMN reply_total SET NOT NULL;
ALTER TABLE t_comment ALTER COLUMN like_total SET DEFAULT 0;
ALTER TABLE t_comment ALTER COLUMN like_total SET NOT NULL;

ALTER TABLE t_mq_send_fail
    ADD COLUMN IF NOT EXISTS orderly BOOLEAN NOT NULL DEFAULT FALSE;

ALTER TABLE t_mq_send_fail
    ADD COLUMN IF NOT EXISTS hash_key VARCHAR(128) NOT NULL DEFAULT '';

CREATE INDEX IF NOT EXISTS idx_comment_note_root_heat
    ON t_comment(note_id, level, is_top DESC, heat DESC, id DESC);

CREATE INDEX IF NOT EXISTS idx_comment_parent_child_time
    ON t_comment(parent_id, level, create_time ASC, id ASC);

CREATE INDEX IF NOT EXISTS idx_comment_reply_tree
    ON t_comment(reply_comment_id, id);

CREATE INDEX IF NOT EXISTS idx_comment_like_comment_id
    ON t_comment_like(comment_id);

WITH root_stats AS (
    SELECT root.id,
           COUNT(child.id) AS reply_total,
           COALESCE((ARRAY_AGG(child.id ORDER BY child.create_time, child.id)
               FILTER (WHERE child.id IS NOT NULL))[1], 0) AS first_reply_comment_id
    FROM t_comment root
    LEFT JOIN t_comment child ON child.parent_id = root.id AND child.level = 2
    WHERE root.level = 1
    GROUP BY root.id
)
UPDATE t_comment root
SET reply_total = stats.reply_total,
    first_reply_comment_id = stats.first_reply_comment_id,
    heat = ROUND(COALESCE(root.like_total, 0) * 0.70 + stats.reply_total * 0.30, 2)
FROM root_stats stats
WHERE root.id = stats.id;

COMMENT ON COLUMN t_comment.first_reply_comment_id IS '最早回复评论 ID（仅一级评论，无则 0）';
COMMENT ON COLUMN t_comment.heat IS '评论热度（点赞 70% + 回复 30%，仅一级评论）';
COMMENT ON COLUMN t_mq_send_fail.orderly IS '是否为顺序消息';
COMMENT ON COLUMN t_mq_send_fail.hash_key IS '顺序消息分片键';
