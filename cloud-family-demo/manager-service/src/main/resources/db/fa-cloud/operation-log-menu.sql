USE `fa-cloud`;

INSERT IGNORE INTO sys_menu (parent_id, menu_code, menu_name, path, component, icon, menu_level, button_flag, sort_order, visible, status)
SELECT parent.id,
       'system-operation-logs',
       '操作日志',
       '/api/manager/system/operation-logs',
       'SystemOperationLogs',
       'ScrollText',
       2,
       0,
       35,
       1,
       'ENABLED'
FROM sys_menu parent
WHERE parent.menu_code = 'system-management';

INSERT IGNORE INTO sys_role_menu (role_id, menu_id)
SELECT r.id, m.id
FROM sys_role r, sys_menu m
WHERE r.role_code = 'MANAGER'
  AND m.menu_code = 'system-operation-logs';

INSERT IGNORE INTO sys_role_menu (role_id, menu_id)
SELECT r.id, m.id
FROM sys_role r, sys_menu m
WHERE r.role_code = 'SUPER_ADMIN'
  AND m.menu_code = 'system-operation-logs';
