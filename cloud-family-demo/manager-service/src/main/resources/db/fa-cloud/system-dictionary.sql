USE `fa-cloud`;
CREATE TABLE IF NOT EXISTS sys_dict_type (
 id bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID，自增' PRIMARY KEY,
 type_code varchar(64) NOT NULL COMMENT '字典类型编码，全局唯一，创建后不可修改',
 parent_type_code varchar(64) DEFAULT NULL COMMENT '上级字典类型编码，空表示大类或独立字典，创建后不可修改',
 name varchar(100) NOT NULL COMMENT '字典类型名称',
 description varchar(500) DEFAULT NULL COMMENT '字典类型说明',
 status varchar(16) NOT NULL DEFAULT 'ENABLED' COMMENT '状态：ENABLED启用，DISABLED停用',
 sort_order int NOT NULL DEFAULT 0 COMMENT '排序值，非负整数，越小越靠前',
 created_at timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
 updated_at timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '最后更新时间',
 UNIQUE KEY uk_dict_type_code(type_code),
 KEY idx_dict_parent(parent_type_code),
 CONSTRAINT fk_dict_parent FOREIGN KEY(parent_type_code) REFERENCES sys_dict_type(type_code) ON DELETE RESTRICT ON UPDATE RESTRICT,
 CONSTRAINT chk_dict_type_status CHECK(status IN ('ENABLED','DISABLED')),
 CONSTRAINT chk_dict_type_sort CHECK(sort_order>=0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='通用业务字典类型';
CREATE TABLE IF NOT EXISTS sys_dict_item (
 id bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID，自增' PRIMARY KEY,
 type_code varchar(64) NOT NULL COMMENT '所属字典类型编码，关联sys_dict_type.type_code，创建后不可修改',
 item_code varchar(64) NOT NULL COMMENT '字典项编码，同一类型内唯一，创建后不可修改',
 name varchar(100) NOT NULL COMMENT '字典项名称，用于页面展示',
 description varchar(500) DEFAULT NULL COMMENT '字典项说明',
 status varchar(16) NOT NULL DEFAULT 'ENABLED' COMMENT '状态：ENABLED启用，DISABLED停用',
 sort_order int NOT NULL DEFAULT 0 COMMENT '排序值，非负整数，越小越靠前',
 created_at timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
 updated_at timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '最后更新时间',
 UNIQUE KEY uk_dict_item_code(type_code,item_code),
 KEY idx_dict_options(type_code,status,sort_order,id),
 CONSTRAINT fk_dict_item_type FOREIGN KEY(type_code) REFERENCES sys_dict_type(type_code) ON DELETE RESTRICT ON UPDATE RESTRICT,
 CONSTRAINT chk_dict_item_status CHECK(status IN ('ENABLED','DISABLED')),
 CONSTRAINT chk_dict_item_sort CHECK(sort_order>=0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='通用业务字典项';

START TRANSACTION;
INSERT IGNORE INTO sys_permission(permission_code,permission_name,description,status) VALUES
('system:dict:view','字典查询','查询字典类型和字典项','ENABLED'),
('system:dict:add','字典新增','新增字典类型和字典项','ENABLED'),
('system:dict:edit','字典修改','修改和启停字典','ENABLED'),
('system:dict:delete','字典删除','删除字典项及空类型','ENABLED');
INSERT IGNORE INTO sys_menu(menu_code,menu_name,path,component,icon,menu_level,button_flag,sort_order,visible,status)
VALUES('function-management','Function Management','/api/manager/function','FunctionManagement','Grid2X2',1,0,50,1,'ENABLED');
INSERT IGNORE INTO sys_menu(parent_id,menu_code,menu_name,path,component,icon,menu_level,button_flag,sort_order,visible,status)
SELECT id,'function-dictionary','字典管理','/api/manager/function/dictionaries','DictionaryManagement','BookOpen',2,0,53,1,'ENABLED' FROM sys_menu WHERE menu_code='function-management';
INSERT IGNORE INTO sys_menu(parent_id,menu_code,menu_name,path,component,icon,menu_level,button_flag,sort_order,visible,status)
SELECT parent.id,child.code,child.name,child.path,'DictionaryPage','ListTree',3,0,child.sort_order,1,'ENABLED'
FROM sys_menu parent JOIN (
 SELECT 'dictionary-types' code,'字典类型' name,'/api/manager/system/dictionaries/types' path,531 sort_order
 UNION ALL SELECT 'dictionary-items','字典项','/api/manager/system/dictionaries/items',532
) child ON 1=1 WHERE parent.menu_code='function-dictionary';
INSERT IGNORE INTO sys_menu(parent_id,menu_code,menu_name,path,component,icon,menu_level,button_flag,sort_order,visible,status)
SELECT m.id,CONCAT(m.menu_code,'-',a.action),a.name,CONCAT('system:dict:',a.action),'ButtonPermission','MousePointerClick',4,1,m.sort_order*10+a.ordering,0,'ENABLED'
FROM sys_menu m JOIN (SELECT 'add' action,'新增' name,1 ordering UNION ALL SELECT 'edit','修改',2 UNION ALL SELECT 'delete','删除',3) a ON 1=1 WHERE m.menu_code IN ('dictionary-types','dictionary-items');
INSERT IGNORE INTO sys_menu_permission(menu_id,permission_id)
SELECT m.id,p.id FROM sys_menu m JOIN sys_permission p ON p.permission_code=IF(m.button_flag=1,m.path,'system:dict:view')
WHERE m.menu_code IN ('dictionary-types','dictionary-items') OR m.menu_code LIKE 'dictionary-types-%' OR m.menu_code LIKE 'dictionary-items-%';
INSERT IGNORE INTO sys_role_menu(role_id,menu_id)
SELECT r.id,m.id FROM sys_role r JOIN sys_menu m WHERE r.role_code='SUPER_ADMIN' AND (m.menu_code IN ('function-management','function-dictionary','dictionary-types','dictionary-items') OR m.menu_code LIKE 'dictionary-types-%' OR m.menu_code LIKE 'dictionary-items-%');
INSERT IGNORE INTO sys_role_permission(role_id,permission_id)
SELECT r.id,p.id FROM sys_role r JOIN sys_permission p WHERE r.role_code='SUPER_ADMIN' AND p.permission_code IN ('system:dict:view','system:dict:add','system:dict:edit','system:dict:delete');
COMMIT;
