USE `fa-cloud`;

INSERT IGNORE INTO sys_permission (permission_code, permission_name, description, status)
VALUES
    ('partner:info:add', 'Partner Info Add', 'Allows creating partner info records', 'ENABLED'),
    ('partner:info:edit', 'Partner Info Edit', 'Allows updating partner info records', 'ENABLED'),
    ('partner:info:reset-password', 'Partner Info Reset Password', 'Allows resetting partner info passwords', 'ENABLED'),
    ('partner:info:delete', 'Partner Info Delete', 'Allows deleting partner info records', 'ENABLED'),
    ('partner:feature:add', 'Partner Feature Add', 'Allows creating partner feature records', 'ENABLED'),
    ('partner:feature:edit', 'Partner Feature Edit', 'Allows updating partner feature records', 'ENABLED'),
    ('partner:feature:delete', 'Partner Feature Delete', 'Allows deleting partner feature records', 'ENABLED');

UPDATE sys_permission
SET permission_name = CASE permission_code
    WHEN 'partner:info:add' THEN 'Partner Info Add'
    WHEN 'partner:info:edit' THEN 'Partner Info Edit'
    WHEN 'partner:info:reset-password' THEN 'Partner Info Reset Password'
    WHEN 'partner:info:delete' THEN 'Partner Info Delete'
    WHEN 'partner:feature:add' THEN 'Partner Feature Add'
    WHEN 'partner:feature:edit' THEN 'Partner Feature Edit'
    WHEN 'partner:feature:delete' THEN 'Partner Feature Delete'
    ELSE permission_name
END
WHERE permission_code IN (
    'partner:info:add',
    'partner:info:edit',
    'partner:info:reset-password',
    'partner:info:delete',
    'partner:feature:add',
    'partner:feature:edit',
    'partner:feature:delete'
);

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
SELECT parent.id, item.menu_code, item.menu_name, item.path, item.component, item.icon, 2, 0, item.sort_order, 1, 'ENABLED'
FROM sys_menu parent
JOIN (
    SELECT 'partner-notification-settings' menu_code, 'Notification Settings' menu_name, '/api/manager/partner/settings/notifications' path, 'PartnerNotificationSettings' component, 'Bell' icon, 42 sort_order
    UNION ALL SELECT 'partner-documents', 'Documents', '/api/manager/partner/settings/documents', 'PartnerDocuments', 'FileText', 43
    UNION ALL SELECT 'partner-versions', 'Versions', '/api/manager/partner/settings/versions', 'PartnerVersions', 'RefreshCw', 44
    UNION ALL SELECT 'partner-memberships', 'Memberships', '/api/manager/partner/memberships', 'PartnerMemberships', 'Crown', 45
    UNION ALL SELECT 'partner-histories', 'Histories', '/api/manager/partner/histories', 'PartnerHistories', 'ScrollText', 46
    UNION ALL SELECT 'partner-favorites', 'Favorites', '/api/manager/partner/favorites', 'PartnerFavorites', 'Star', 47
) item ON 1 = 1
WHERE parent.menu_code = 'partner-management';

UPDATE sys_menu
SET menu_name = CASE menu_code
    WHEN 'partner-notification-settings' THEN 'Notification Settings'
    WHEN 'partner-documents' THEN 'Documents'
    WHEN 'partner-versions' THEN 'Versions'
    WHEN 'partner-memberships' THEN 'Memberships'
    WHEN 'partner-histories' THEN 'Histories'
    WHEN 'partner-favorites' THEN 'Favorites'
    ELSE menu_name
END
WHERE menu_code IN (
    'partner-notification-settings',
    'partner-documents',
    'partner-versions',
    'partner-memberships',
    'partner-histories',
    'partner-favorites'
);

INSERT IGNORE INTO sys_menu (parent_id, menu_code, menu_name, path, component, icon, menu_level, button_flag, sort_order, visible, status)
SELECT parent.id, item.menu_code, item.menu_name, item.permission_code, 'ButtonPermission', 'MousePointerClick', 3, 1, item.sort_order, 0, 'ENABLED'
FROM sys_menu parent
JOIN (
    SELECT 'partner-info-add' menu_code, 'Add' menu_name, 'partner:info:add' permission_code, 411 sort_order
    UNION ALL SELECT 'partner-info-edit', 'Edit', 'partner:info:edit', 412
    UNION ALL SELECT 'partner-info-reset-password', 'Reset Password', 'partner:info:reset-password', 413
    UNION ALL SELECT 'partner-info-delete', 'Delete', 'partner:info:delete', 414
) item ON 1 = 1
WHERE parent.menu_code = 'partner-info';

INSERT IGNORE INTO sys_menu (parent_id, menu_code, menu_name, path, component, icon, menu_level, button_flag, sort_order, visible, status)
SELECT parent.id, CONCAT(parent.menu_code, '-', item.action_code), item.menu_name, item.permission_code, 'ButtonPermission', 'MousePointerClick', 3, 1, parent.sort_order * 10 + item.sort_offset, 0, 'ENABLED'
FROM sys_menu parent
JOIN (
    SELECT 'add' action_code, 'Add' menu_name, 'partner:feature:add' permission_code, 1 sort_offset
    UNION ALL SELECT 'edit', 'Edit', 'partner:feature:edit', 2
    UNION ALL SELECT 'delete', 'Delete', 'partner:feature:delete', 3
) item ON 1 = 1
WHERE parent.menu_code IN (
    'partner-notification-settings',
    'partner-documents',
    'partner-versions',
    'partner-memberships',
    'partner-histories',
    'partner-favorites'
);

UPDATE sys_menu
SET menu_name = CASE menu_code
    WHEN 'partner-info-add' THEN 'Add'
    WHEN 'partner-info-edit' THEN 'Edit'
    WHEN 'partner-info-reset-password' THEN 'Reset Password'
    WHEN 'partner-info-delete' THEN 'Delete'
    ELSE menu_name
END
WHERE menu_code IN ('partner-info-add', 'partner-info-edit', 'partner-info-reset-password', 'partner-info-delete');

UPDATE sys_menu
SET menu_name = CASE
    WHEN menu_code LIKE '%-add' THEN 'Add'
    WHEN menu_code LIKE '%-edit' THEN 'Edit'
    WHEN menu_code LIKE '%-delete' THEN 'Delete'
    ELSE menu_name
END
WHERE button_flag = 1
  AND (
      menu_code LIKE 'partner-notification-settings-%'
      OR menu_code LIKE 'partner-documents-%'
      OR menu_code LIKE 'partner-versions-%'
      OR menu_code LIKE 'partner-memberships-%'
      OR menu_code LIKE 'partner-histories-%'
      OR menu_code LIKE 'partner-favorites-%'
  );

INSERT IGNORE INTO sys_menu_permission (menu_id, permission_id)
SELECT m.id, p.id
FROM sys_menu m
INNER JOIN sys_permission p ON p.permission_code = m.path
WHERE m.menu_code IN ('partner-info-add', 'partner-info-edit', 'partner-info-reset-password', 'partner-info-delete');

INSERT IGNORE INTO sys_menu_permission (menu_id, permission_id)
SELECT m.id, p.id
FROM sys_menu m
INNER JOIN sys_permission p ON p.permission_code = m.path
WHERE m.menu_code LIKE 'partner-%'
  AND m.button_flag = 1
  AND p.permission_code IN ('partner:feature:add', 'partner:feature:edit', 'partner:feature:delete');

DELETE rp
FROM sys_role_permission rp
INNER JOIN sys_role r ON r.id = rp.role_id
INNER JOIN sys_permission p ON p.id = rp.permission_id
WHERE r.role_code = 'MANAGER'
  AND p.permission_code IN ('partner:info:add', 'partner:info:edit', 'partner:info:reset-password', 'partner:info:delete');

DELETE rm
FROM sys_role_menu rm
INNER JOIN sys_role r ON r.id = rm.role_id
INNER JOIN sys_menu m ON m.id = rm.menu_id
WHERE r.role_code = 'MANAGER'
  AND m.menu_code IN ('partner-management', 'partner-info', 'partner-info-add', 'partner-info-edit', 'partner-info-reset-password', 'partner-info-delete');

INSERT IGNORE INTO sys_role_permission (role_id, permission_id)
SELECT r.id, p.id
FROM sys_role r, sys_permission p
WHERE r.role_code = 'SUPER_ADMIN'
  AND p.permission_code IN ('partner:info:add', 'partner:info:edit', 'partner:info:reset-password', 'partner:info:delete', 'partner:feature:add', 'partner:feature:edit', 'partner:feature:delete');

INSERT IGNORE INTO sys_role_permission (role_id, permission_id)
SELECT r.id, p.id
FROM sys_role r, sys_permission p
WHERE r.role_code = 'MANAGER'
  AND p.permission_code IN ('partner:feature:add', 'partner:feature:edit', 'partner:feature:delete');

INSERT IGNORE INTO sys_role_menu (role_id, menu_id)
SELECT r.id, m.id
FROM sys_role r, sys_menu m
WHERE r.role_code = 'SUPER_ADMIN'
  AND (
      m.menu_code IN ('partner-management', 'partner-info', 'partner-info-add', 'partner-info-edit', 'partner-info-reset-password', 'partner-info-delete')
      OR m.menu_code IN ('partner-notification-settings', 'partner-documents', 'partner-versions', 'partner-memberships', 'partner-histories', 'partner-favorites')
      OR m.menu_code LIKE 'partner-notification-settings-%'
      OR m.menu_code LIKE 'partner-documents-%'
      OR m.menu_code LIKE 'partner-versions-%'
      OR m.menu_code LIKE 'partner-memberships-%'
      OR m.menu_code LIKE 'partner-histories-%'
      OR m.menu_code LIKE 'partner-favorites-%'
  );

INSERT IGNORE INTO sys_role_menu (role_id, menu_id)
SELECT r.id, m.id
FROM sys_role r, sys_menu m
WHERE r.role_code = 'MANAGER'
  AND (
      m.menu_code IN ('partner-management', 'partner-notification-settings', 'partner-documents', 'partner-versions', 'partner-memberships', 'partner-histories', 'partner-favorites')
      OR m.menu_code LIKE 'partner-notification-settings-%'
      OR m.menu_code LIKE 'partner-documents-%'
      OR m.menu_code LIKE 'partner-versions-%'
      OR m.menu_code LIKE 'partner-memberships-%'
      OR m.menu_code LIKE 'partner-histories-%'
      OR m.menu_code LIKE 'partner-favorites-%'
  );
