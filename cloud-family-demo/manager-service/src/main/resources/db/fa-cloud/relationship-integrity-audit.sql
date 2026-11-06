-- 在 fa-cloud 或对应测试库执行；所有 orphan_count 应为0，仅查询，不修复或删除数据。
SELECT 'sys_menu.parent_id' AS relation_name,COUNT(*) AS orphan_count FROM sys_menu c LEFT JOIN sys_menu p ON p.id=c.parent_id WHERE c.parent_id IS NOT NULL AND p.id IS NULL
UNION ALL SELECT 'sys_user_role.user_id',COUNT(*) FROM sys_user_role c LEFT JOIN sys_user p ON p.id=c.user_id WHERE p.id IS NULL
UNION ALL SELECT 'sys_user_role.role_id',COUNT(*) FROM sys_user_role c LEFT JOIN sys_role p ON p.id=c.role_id WHERE p.id IS NULL
UNION ALL SELECT 'sys_role_permission.role_id',COUNT(*) FROM sys_role_permission c LEFT JOIN sys_role p ON p.id=c.role_id WHERE p.id IS NULL
UNION ALL SELECT 'sys_role_permission.permission_id',COUNT(*) FROM sys_role_permission c LEFT JOIN sys_permission p ON p.id=c.permission_id WHERE p.id IS NULL
UNION ALL SELECT 'sys_role_menu.role_id',COUNT(*) FROM sys_role_menu c LEFT JOIN sys_role p ON p.id=c.role_id WHERE p.id IS NULL
UNION ALL SELECT 'sys_role_menu.menu_id',COUNT(*) FROM sys_role_menu c LEFT JOIN sys_menu p ON p.id=c.menu_id WHERE p.id IS NULL
UNION ALL SELECT 'sys_menu_permission.menu_id',COUNT(*) FROM sys_menu_permission c LEFT JOIN sys_menu p ON p.id=c.menu_id WHERE p.id IS NULL
UNION ALL SELECT 'sys_menu_permission.permission_id',COUNT(*) FROM sys_menu_permission c LEFT JOIN sys_permission p ON p.id=c.permission_id WHERE p.id IS NULL
UNION ALL SELECT 'sys_dict_type.parent_type_code',COUNT(*) FROM sys_dict_type c LEFT JOIN sys_dict_type p ON p.type_code=c.parent_type_code WHERE c.parent_type_code IS NOT NULL AND p.id IS NULL
UNION ALL SELECT 'sys_dict_item.type_code',COUNT(*) FROM sys_dict_item c LEFT JOIN sys_dict_type p ON p.type_code=c.type_code WHERE p.id IS NULL
UNION ALL SELECT 'showcase_shop_type_rel.shop_id',COUNT(*) FROM showcase_shop_type_rel c LEFT JOIN showcase_shop p ON p.id=c.shop_id WHERE p.id IS NULL
UNION ALL SELECT 'showcase_shop_type_rel.type_code',COUNT(*) FROM showcase_shop_type_rel c LEFT JOIN showcase_shop_type p ON p.type_code=c.type_code WHERE p.id IS NULL
UNION ALL SELECT 'showcase_product.shop_id',COUNT(*) FROM showcase_product c LEFT JOIN showcase_shop p ON p.id=c.shop_id WHERE p.id IS NULL
UNION ALL SELECT 'showcase_product.type_id',COUNT(*) FROM showcase_product c LEFT JOIN showcase_shop_type p ON p.id=c.type_id WHERE c.type_id IS NOT NULL AND p.id IS NULL;
