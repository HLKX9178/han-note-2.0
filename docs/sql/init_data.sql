-- hannote RBAC 初始化数据（PostgreSQL）
-- 在 t_role / t_permission / t_role_permission_rel 表创建后执行

-- 普通用户角色（ID 由 IDENTITY 自增产生，这里显式指定以便关联表引用）
INSERT INTO t_role (role_name, role_key, status, sort, remark)
VALUES ('普通用户', 'common_user', 0, 1, '小憨书 APP 端普通用户');

-- APP 端按钮权限
INSERT INTO t_permission (parent_id, name, type, menu_url, menu_icon, sort, permission_key, status)
VALUES (0, '发布笔记', 3, '', '', 1, 'app:note:publish', 0);

INSERT INTO t_permission (parent_id, name, type, menu_url, menu_icon, sort, permission_key, status)
VALUES (0, '发布评论', 3, '', '', 2, 'app:comment:publish', 0);

-- 角色-权限关联（假设上述 INSERT 后 ID 依次为 1 / 1 / 2，可根据实际情况调整）
INSERT INTO t_role_permission_rel (role_id, permission_id) VALUES (1, 1);
INSERT INTO t_role_permission_rel (role_id, permission_id) VALUES (1, 2);

-- ============================================================================
-- 笔记服务：频道 / 话题 / 关联 种子数据（自测前置）
-- t_channel / t_topic / t_channel_topic_rel 主键为 BIGINT（正常由分布式 ID 服务生成），
-- 这里用固定小 ID 便于本地测试；生产环境请勿沿用。
-- ============================================================================

-- 频道：美食(1)、娱乐(2)
INSERT INTO t_channel (id, name) VALUES (1, '美食');
INSERT INTO t_channel (id, name) VALUES (2, '娱乐');

-- 话题：高分美剧推荐(1)、下饭综艺推荐(2)
INSERT INTO t_topic (id, name) VALUES (1, '高分美剧推荐');
INSERT INTO t_topic (id, name) VALUES (2, '下饭综艺推荐');

-- 频道-话题关联：两条话题都挂到娱乐频道(2)下
INSERT INTO t_channel_topic_rel (id, channel_id, topic_id) VALUES (1, 2, 1);
INSERT INTO t_channel_topic_rel (id, channel_id, topic_id) VALUES (2, 2, 2);
