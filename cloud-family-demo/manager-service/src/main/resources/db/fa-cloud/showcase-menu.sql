USE `fa-cloud`;

-- 可重复执行，仅向现有 SUPER_ADMIN / MANAGER 角色添加精品模块授权。
INSERT IGNORE INTO sys_permission (permission_code, permission_name, description, status)
VALUES ('partner:showcase:view', '精品展示查询', '查看精品展示管理列表', 'ENABLED'),
       ('partner:showcase:add', '精品展示新增', '创建精品内容', 'ENABLED'),
       ('partner:showcase:edit', '精品展示修改', '编辑精品及上下架', 'ENABLED'),
       ('partner:showcase:delete', '精品展示删除', '删除精品内容', 'ENABLED');

INSERT IGNORE INTO sys_menu (menu_code, menu_name, path, component, icon, menu_level, button_flag, sort_order, visible, status)
VALUES ('partner-management', 'Partner Management', '/api/manager/partner', 'PartnerManagement', 'Handshake', 1, 0, 40, 1, 'ENABLED');

INSERT IGNORE INTO sys_menu (parent_id, menu_code, menu_name, path, component, icon, menu_level, button_flag, sort_order, visible, status)
SELECT id, 'partner-showcase', '精品展示', '/api/manager/partner/showcase', 'PartnerShowcase', 'Gift', 2, 0, 48, 1, 'ENABLED'
FROM sys_menu WHERE menu_code = 'partner-management';

INSERT IGNORE INTO sys_menu (parent_id, menu_code, menu_name, path, component, icon, menu_level, button_flag, sort_order, visible, status)
SELECT parent.id, CONCAT('partner-showcase-', item.action), item.label, CONCAT('partner:showcase:', item.action),
       'ButtonPermission', 'MousePointerClick', 3, 1, item.sort_order, 0, 'ENABLED'
FROM sys_menu parent
JOIN (
    SELECT 'add' action, '新增' label, 481 sort_order
    UNION ALL SELECT 'edit', '修改', 482
    UNION ALL SELECT 'delete', '删除', 483
) item ON 1 = 1
WHERE parent.menu_code = 'partner-showcase';

INSERT IGNORE INTO sys_menu_permission (menu_id, permission_id)
SELECT m.id, p.id FROM sys_menu m JOIN sys_permission p
ON p.permission_code = CASE WHEN m.button_flag = 1 THEN m.path ELSE 'partner:showcase:view' END
WHERE m.menu_code = 'partner-showcase' OR m.menu_code LIKE 'partner-showcase-%';

INSERT IGNORE INTO sys_role_permission (role_id, permission_id)
SELECT r.id, p.id FROM sys_role r JOIN sys_permission p ON p.permission_code LIKE 'partner:showcase:%'
WHERE r.role_code IN ('SUPER_ADMIN', 'MANAGER');

INSERT IGNORE INTO sys_role_menu (role_id, menu_id)
SELECT r.id, m.id FROM sys_role r JOIN sys_menu m
ON m.menu_code IN ('partner-management', 'partner-showcase') OR m.menu_code LIKE 'partner-showcase-%'
WHERE r.role_code IN ('SUPER_ADMIN', 'MANAGER');
