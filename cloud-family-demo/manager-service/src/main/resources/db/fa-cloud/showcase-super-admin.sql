USE `fa-cloud`;

-- 为超级管理员补齐精品展示的查询、新增、修改、删除权限和菜单。
INSERT IGNORE INTO sys_role_permission (role_id, permission_id)
SELECT r.id, p.id
FROM sys_role r JOIN sys_permission p
  ON p.permission_code IN ('partner:showcase:view', 'partner:showcase:add', 'partner:showcase:edit', 'partner:showcase:delete','partner:shop-type:view','partner:shop-type:add','partner:shop-type:edit','partner:shop-type:delete')
WHERE r.role_code = 'SUPER_ADMIN';

INSERT IGNORE INTO sys_role_menu (role_id, menu_id)
SELECT r.id, m.id
FROM sys_role r JOIN sys_menu m
  ON m.menu_code IN ('partner-management', 'partner-showcase', 'partner-showcase-add', 'partner-showcase-edit', 'partner-showcase-delete', 'partner-shops', 'partner-shops-add', 'partner-shops-edit', 'partner-shops-delete', 'partner-products', 'partner-products-add', 'partner-products-edit', 'partner-products-delete','partner-shop-types','partner-shop-types-add','partner-shop-types-edit','partner-shop-types-delete')
WHERE r.role_code = 'SUPER_ADMIN';
