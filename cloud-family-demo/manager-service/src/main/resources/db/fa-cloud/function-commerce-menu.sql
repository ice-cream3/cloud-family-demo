USE `fa-cloud`;

-- 在基础菜单、精品展示、商家/商品及商家类型菜单初始化后执行，可重复执行。
-- 保留页面 ID、路由及原有角色授权，只调整菜单树并补齐 SUPER_ADMIN 授权。
START TRANSACTION;

INSERT IGNORE INTO sys_menu(menu_code,menu_name,path,component,icon,menu_level,button_flag,sort_order,visible,status)
VALUES('function-management','Function Management','/api/manager/function','FunctionManagement','Grid2X2',1,0,50,1,'ENABLED');

INSERT IGNORE INTO sys_menu(parent_id,menu_code,menu_name,path,component,icon,menu_level,button_flag,sort_order,visible,status)
SELECT id,'function-commerce-management','商家管理','/api/manager/function/commerce','CommerceManagement','Store',2,0,52,1,'ENABLED'
FROM sys_menu WHERE menu_code='function-management';

UPDATE sys_menu group_menu JOIN sys_menu root_menu ON root_menu.menu_code='function-management'
SET group_menu.parent_id=root_menu.id,group_menu.menu_name='商家管理',group_menu.menu_level=2,
    group_menu.button_flag=0,group_menu.visible=1,group_menu.status='ENABLED'
WHERE group_menu.menu_code='function-commerce-management';

UPDATE sys_menu page_menu JOIN sys_menu group_menu ON group_menu.menu_code='function-commerce-management'
SET page_menu.parent_id=group_menu.id,page_menu.menu_level=3,page_menu.button_flag=0,
    page_menu.visible=1,page_menu.status='ENABLED',
    page_menu.menu_name=CASE page_menu.menu_code
      WHEN 'partner-shop-types' THEN '商家类型管理'
      WHEN 'partner-shops' THEN '商家管理'
      WHEN 'partner-products' THEN '商品管理'
      WHEN 'partner-showcase' THEN '精品展示' END,
    page_menu.sort_order=CASE page_menu.menu_code
      WHEN 'partner-shop-types' THEN 521 WHEN 'partner-shops' THEN 522
      WHEN 'partner-products' THEN 523 WHEN 'partner-showcase' THEN 524 END
WHERE page_menu.menu_code IN ('partner-shop-types','partner-shops','partner-products','partner-showcase');

UPDATE sys_menu button_menu JOIN sys_menu page_menu ON page_menu.id=button_menu.parent_id
SET button_menu.menu_level=4
WHERE page_menu.menu_code IN ('partner-shop-types','partner-shops','partner-products','partner-showcase')
  AND button_menu.button_flag=1;

INSERT IGNORE INTO sys_menu_permission(menu_id,permission_id)
SELECT m.id,p.id FROM sys_menu m JOIN sys_permission p
  ON p.permission_code=CASE WHEN m.button_flag=1 THEN m.path
    WHEN m.menu_code='partner-shop-types' THEN 'partner:shop-type:view' ELSE 'partner:showcase:view' END
WHERE m.menu_code IN ('partner-shop-types','partner-shops','partner-products','partner-showcase')
   OR (m.button_flag=1 AND m.parent_id IN (
       SELECT id FROM (SELECT id FROM sys_menu WHERE menu_code IN ('partner-shop-types','partner-shops','partner-products','partner-showcase')) pages));

INSERT IGNORE INTO sys_role_menu(role_id,menu_id)
SELECT r.id,m.id FROM sys_role r JOIN sys_menu m
WHERE r.role_code='SUPER_ADMIN'
  AND (m.menu_code IN ('function-management','function-commerce-management','partner-shop-types','partner-shops','partner-products','partner-showcase')
       OR m.parent_id IN (SELECT id FROM (SELECT id FROM sys_menu WHERE menu_code IN ('partner-shop-types','partner-shops','partner-products','partner-showcase')) pages));

INSERT IGNORE INTO sys_role_permission(role_id,permission_id)
SELECT r.id,mp.permission_id FROM sys_role r JOIN sys_menu_permission mp JOIN sys_menu m ON m.id=mp.menu_id
WHERE r.role_code='SUPER_ADMIN'
  AND (m.menu_code IN ('partner-shop-types','partner-shops','partner-products','partner-showcase')
       OR m.parent_id IN (SELECT id FROM (SELECT id FROM sys_menu WHERE menu_code IN ('partner-shop-types','partner-shops','partner-products','partner-showcase')) pages));

COMMIT;
