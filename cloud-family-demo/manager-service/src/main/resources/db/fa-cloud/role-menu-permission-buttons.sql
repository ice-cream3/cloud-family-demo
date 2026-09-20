USE `fa-cloud`;

INSERT IGNORE INTO sys_permission (permission_code, permission_name, description, status)
VALUES
    ('system:role:menu', '分配菜单', 'Allows configuring role menus', 'ENABLED');

UPDATE sys_permission
SET permission_name = '分配权限'
WHERE permission_code = 'system:menu:button-permission';

INSERT IGNORE INTO sys_menu (parent_id, menu_code, menu_name, path, component, icon, menu_level, button_flag, sort_order, visible, status)
SELECT parent.id, 'system-roles-menu', '分配菜单', 'system:role:menu', 'ButtonPermission', 'MousePointerClick', parent.menu_level + 1, 1, 324, 0, 'ENABLED'
FROM sys_menu parent
WHERE parent.menu_code = 'system-roles';

UPDATE sys_menu
SET menu_name = '分配权限'
WHERE menu_code = 'system-menus-button-permission';

INSERT IGNORE INTO sys_menu_permission (menu_id, permission_id)
SELECT m.id, p.id
FROM sys_menu m
INNER JOIN sys_permission p ON p.permission_code = m.path
WHERE m.component = 'ButtonPermission';

INSERT IGNORE INTO sys_role_menu (role_id, menu_id)
SELECT r.id, m.id
FROM sys_role r, sys_menu m
WHERE r.role_code = 'SUPER_ADMIN'
  AND m.menu_code = 'system-roles-menu';
