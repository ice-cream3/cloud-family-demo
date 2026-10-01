USE `fa-cloud`;

INSERT IGNORE INTO sys_permission (permission_code, permission_name, description, status)
VALUES
    ('partner:info:add', 'Partner Info 新增', 'Allows creating partner info records', 'ENABLED'),
    ('partner:info:edit', 'Partner Info 修改', 'Allows updating partner info records', 'ENABLED'),
    ('partner:info:delete', 'Partner Info 删除', 'Allows deleting partner info records', 'ENABLED');

INSERT IGNORE INTO sys_menu (menu_code, menu_name, path, component, icon, menu_level, button_flag, sort_order, visible, status)
VALUES ('partner-management', 'Partner Management', '/api/manager/partner', 'PartnerManagement', 'Handshake', 1, 0, 40, 1, 'ENABLED');

INSERT IGNORE INTO sys_menu (parent_id, menu_code, menu_name, path, component, icon, menu_level, button_flag, sort_order, visible, status)
SELECT parent.id,
       'partner-info',
       'Partner Info',
       '/api/manager/partner/info',
       'PartnerInfo',
       'UsersRound',
       2,
       0,
       41,
       1,
       'ENABLED'
FROM sys_menu parent
WHERE parent.menu_code = 'partner-management';

INSERT IGNORE INTO sys_menu (parent_id, menu_code, menu_name, path, component, icon, menu_level, button_flag, sort_order, visible, status)
SELECT parent.id, item.menu_code, item.menu_name, item.permission_code, 'ButtonPermission', 'MousePointerClick', 3, 1, item.sort_order, 0, 'ENABLED'
FROM sys_menu parent
JOIN (
    SELECT 'partner-info-add' menu_code, '新增' menu_name, 'partner:info:add' permission_code, 411 sort_order
    UNION ALL SELECT 'partner-info-edit', '修改', 'partner:info:edit', 412
    UNION ALL SELECT 'partner-info-delete', '删除', 'partner:info:delete', 413
) item ON 1 = 1
WHERE parent.menu_code = 'partner-info';

INSERT IGNORE INTO sys_menu_permission (menu_id, permission_id)
SELECT m.id, p.id
FROM sys_menu m
INNER JOIN sys_permission p ON p.permission_code = m.path
WHERE m.menu_code IN ('partner-info-add', 'partner-info-edit', 'partner-info-delete');

DELETE rp
FROM sys_role_permission rp
INNER JOIN sys_role r ON r.id = rp.role_id
INNER JOIN sys_permission p ON p.id = rp.permission_id
WHERE r.role_code = 'MANAGER'
  AND p.permission_code IN ('partner:info:add', 'partner:info:edit', 'partner:info:delete');

DELETE rm
FROM sys_role_menu rm
INNER JOIN sys_role r ON r.id = rm.role_id
INNER JOIN sys_menu m ON m.id = rm.menu_id
WHERE r.role_code = 'MANAGER'
  AND m.menu_code IN ('partner-management', 'partner-info', 'partner-info-add', 'partner-info-edit', 'partner-info-delete');

INSERT IGNORE INTO sys_role_permission (role_id, permission_id)
SELECT r.id, p.id
FROM sys_role r, sys_permission p
WHERE r.role_code = 'SUPER_ADMIN'
  AND p.permission_code IN ('partner:info:add', 'partner:info:edit', 'partner:info:delete');

INSERT IGNORE INTO sys_role_menu (role_id, menu_id)
SELECT r.id, m.id
FROM sys_role r, sys_menu m
WHERE r.role_code = 'SUPER_ADMIN'
  AND m.menu_code IN ('partner-management', 'partner-info', 'partner-info-add', 'partner-info-edit', 'partner-info-delete');
