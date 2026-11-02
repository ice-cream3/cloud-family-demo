USE `fa-cloud`;

-- 沿用精品展示独立权限，SUPER_ADMIN 与 MANAGER 可管理所有用户的店铺和商品。
INSERT IGNORE INTO sys_menu(parent_id,menu_code,menu_name,path,component,icon,menu_level,button_flag,sort_order,visible,status)
SELECT id,'partner-shops','商家管理','/api/manager/partner/shops','PartnerShops','Store',2,0,49,1,'ENABLED'
FROM sys_menu WHERE menu_code='partner-management';
INSERT IGNORE INTO sys_menu(parent_id,menu_code,menu_name,path,component,icon,menu_level,button_flag,sort_order,visible,status)
SELECT id,'partner-products','商品管理','/api/manager/partner/products','PartnerProducts','Package',2,0,50,1,'ENABLED'
FROM sys_menu WHERE menu_code='partner-management';

INSERT IGNORE INTO sys_menu(parent_id,menu_code,menu_name,path,component,icon,menu_level,button_flag,sort_order,visible,status)
SELECT m.id,CONCAT(m.menu_code,'-',a.action),a.label,CONCAT('partner:showcase:',a.action),'ButtonPermission','MousePointerClick',3,1,m.sort_order*10+a.sort_offset,0,'ENABLED'
FROM sys_menu m JOIN (
 SELECT 'add' action,'新增' label,1 sort_offset UNION ALL SELECT 'edit','修改',2 UNION ALL SELECT 'delete','删除',3
) a ON 1=1 WHERE m.menu_code IN ('partner-shops','partner-products');

INSERT IGNORE INTO sys_menu_permission(menu_id,permission_id)
SELECT m.id,p.id FROM sys_menu m JOIN sys_permission p ON p.permission_code=CASE WHEN m.button_flag=1 THEN m.path ELSE 'partner:showcase:view' END
WHERE m.menu_code IN ('partner-shops','partner-products') OR m.menu_code LIKE 'partner-shops-%' OR m.menu_code LIKE 'partner-products-%';
INSERT IGNORE INTO sys_role_permission(role_id,permission_id)
SELECT r.id,p.id FROM sys_role r JOIN sys_permission p ON p.permission_code IN ('partner:showcase:view','partner:showcase:add','partner:showcase:edit','partner:showcase:delete')
WHERE r.role_code IN ('SUPER_ADMIN','MANAGER');
INSERT IGNORE INTO sys_role_menu(role_id,menu_id)
SELECT r.id,m.id FROM sys_role r JOIN sys_menu m ON m.menu_code IN ('partner-management','partner-shops','partner-products') OR m.menu_code LIKE 'partner-shops-%' OR m.menu_code LIKE 'partner-products-%'
WHERE r.role_code IN ('SUPER_ADMIN','MANAGER');
UPDATE sys_menu SET menu_name='精品内容（旧版）' WHERE menu_code='partner-showcase';
