USE `fa-cloud`;

INSERT IGNORE INTO sys_permission(permission_code,permission_name,description,status)
VALUES('partner:membership:reset-password','重置会员密码','允许在会员中心重置关联会员账号密码','ENABLED');
INSERT IGNORE INTO sys_menu(parent_id,menu_code,menu_name,path,component,icon,menu_level,button_flag,sort_order,visible,status)
SELECT id,'partner-memberships-reset-password','重置会员密码','partner:membership:reset-password','ButtonPermission','KeyRound',3,1,459,0,'ENABLED'
FROM sys_menu WHERE path='/api/manager/partner/memberships' AND button_flag=0;
INSERT IGNORE INTO sys_menu_permission(menu_id,permission_id)
SELECT m.id,p.id FROM sys_menu m JOIN sys_permission p ON p.permission_code='partner:membership:reset-password'
WHERE m.menu_code='partner-memberships-reset-password';
INSERT IGNORE INTO sys_role_permission(role_id,permission_id)
SELECT r.id,p.id FROM sys_role r JOIN sys_permission p ON p.permission_code='partner:membership:reset-password'
WHERE r.role_code IN ('SUPER_ADMIN','MANAGER');
INSERT IGNORE INTO sys_role_menu(role_id,menu_id)
SELECT r.id,m.id FROM sys_role r JOIN sys_menu m ON m.menu_code='partner-memberships-reset-password'
WHERE r.role_code IN ('SUPER_ADMIN','MANAGER');
