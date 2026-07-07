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
