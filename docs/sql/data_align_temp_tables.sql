-- ============================================================================
-- 数据对齐服务日增量临时表（PostgreSQL）—— 参考模板
-- ----------------------------------------------------------------------------
-- 说明：这些表由 hannote-data-align 的 CreateDailyTableProcessor 定时任务
--      「动态创建」，表名后缀 = {yyyyMMdd}_{分片序号}（如 _20260711_0）。
--      本文件仅作结构参考/评审用，正常运行无需手动执行。
--      如需手动建当日表联调，把 ${suffix} 替换为具体的「日期_分片序号」即可。
--
-- 关键点：
--   - 主键用 BIGINT GENERATED ALWAYS AS IDENTITY（对应 MySQL bigint AUTO_INCREMENT）；
--   - 唯一约束用「内联匿名 UNIQUE」而非命名约束——动态表名下命名约束在同 schema 全局唯一，
--     会跨表冲突；匿名 UNIQUE 由 PG 自动命名为 <表名>_<列>_key，因表名唯一而不冲突。
-- ============================================================================

-- 关注数变更（源用户 user_id）
CREATE TABLE IF NOT EXISTS t_data_align_following_count_temp_${suffix} (
    id      BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    user_id BIGINT NOT NULL UNIQUE
);

-- 粉丝数变更（目标用户 user_id）
CREATE TABLE IF NOT EXISTS t_data_align_fans_count_temp_${suffix} (
    id      BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    user_id BIGINT NOT NULL UNIQUE
);

-- 笔记被收藏数变更（note_id）
CREATE TABLE IF NOT EXISTS t_data_align_note_collect_count_temp_${suffix} (
    id      BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    note_id BIGINT NOT NULL UNIQUE
);

-- 用户获藏数变更（发布者 user_id）
CREATE TABLE IF NOT EXISTS t_data_align_user_collect_count_temp_${suffix} (
    id      BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    user_id BIGINT NOT NULL UNIQUE
);

-- 用户获赞数变更（发布者 user_id）
CREATE TABLE IF NOT EXISTS t_data_align_user_like_count_temp_${suffix} (
    id      BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    user_id BIGINT NOT NULL UNIQUE
);

-- 笔记被点赞数变更（note_id）
CREATE TABLE IF NOT EXISTS t_data_align_note_like_count_temp_${suffix} (
    id      BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    note_id BIGINT NOT NULL UNIQUE
);

-- 用户发布笔记数变更（发布者 user_id；10415 修正为 user_id 维度）
CREATE TABLE IF NOT EXISTS t_data_align_note_publish_count_temp_${suffix} (
    id      BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    user_id BIGINT NOT NULL UNIQUE
);

-- 笔记评论总数变更（note_id）
CREATE TABLE IF NOT EXISTS t_data_align_note_comment_count_temp_${suffix} (
    id      BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    note_id BIGINT NOT NULL UNIQUE
);

-- ----------------------------------------------------------------------------
-- 对齐任务回写的目标表（已存在，由 hannote-count 属主维护，此处仅备注）：
--   t_user_count(following_total, fans_total, like_total, collect_total, note_total)
--   t_note_count(like_total, collect_total, comment_total)
-- ============================================================================
