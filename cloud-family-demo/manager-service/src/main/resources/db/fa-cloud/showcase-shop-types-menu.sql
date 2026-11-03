USE `fa-cloud`;
INSERT IGNORE INTO sys_permission(permission_code,permission_name,description,status) VALUES
('partner:shop-type:view','商家类型查询','查询商家类型字典','ENABLED'),
('partner:shop-type:add','商家类型新增','新增商家类型','ENABLED'),
('partner:shop-type:edit','商家类型修改','修改、启用和停用商家类型','ENABLED'),
('partner:shop-type:delete','商家类型删除','删除未被使用的商家类型','ENABLED');
INSERT IGNORE INTO sys_menu(parent_id,menu_code,menu_name,path,component,icon,menu_level,button_flag,sort_order,visible,status)
SELECT id,'partner-shop-types','商家类型管理','/api/manager/partner/shop-types','PartnerShopTypes','Tags',2,0,48,1,'ENABLED' FROM sys_menu WHERE menu_code='partner-management';
INSERT IGNORE INTO sys_menu(parent_id,menu_code,menu_name,path,component,icon,menu_level,button_flag,sort_order,visible,status)
SELECT m.id,CONCAT('partner-shop-types-',a.action),a.label,CONCAT('partner:shop-type:',a.action),'ButtonPermission','MousePointerClick',3,1,480+a.sort_order,0,'ENABLED'
FROM sys_menu m JOIN (SELECT 'add' action,'新增' label,1 sort_order UNION ALL SELECT 'edit','修改',2 UNION ALL SELECT 'delete','删除',3) a ON 1=1 WHERE m.menu_code='partner-shop-types';
INSERT IGNORE INTO sys_menu_permission(menu_id,permission_id)
SELECT m.id,p.id FROM sys_menu m JOIN sys_permission p ON p.permission_code=CASE WHEN m.button_flag=1 THEN m.path ELSE 'partner:shop-type:view' END
WHERE m.menu_code='partner-shop-types' OR m.menu_code LIKE 'partner-shop-types-%';
INSERT IGNORE INTO sys_role_permission(role_id,permission_id)
SELECT r.id,p.id FROM sys_role r JOIN sys_permission p ON p.permission_code IN('partner:shop-type:view','partner:shop-type:add','partner:shop-type:edit','partner:shop-type:delete') WHERE r.role_code IN('SUPER_ADMIN','MANAGER');
INSERT IGNORE INTO sys_role_menu(role_id,menu_id)
SELECT r.id,m.id FROM sys_role r JOIN sys_menu m ON m.menu_code='partner-shop-types' OR m.menu_code LIKE 'partner-shop-types-%' WHERE r.role_code IN('SUPER_ADMIN','MANAGER');
