USE `fa-cloud`;
-- 前置：system-dictionary.sql、merchant-type-dictionary.sql。兼容已建表，可重复执行。
SELECT IF(COUNT(*)=0,
 'ALTER TABLE sys_dict_type ADD COLUMN parent_type_code varchar(64) DEFAULT NULL COMMENT ''上级字典类型编码，空表示大类或独立字典，创建后不可修改'' AFTER type_code, ADD KEY idx_dict_parent(parent_type_code)',
 'SELECT 1') INTO @dict_parent_ddl
FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='sys_dict_type' AND column_name='parent_type_code';
PREPARE dict_parent_stmt FROM @dict_parent_ddl;
EXECUTE dict_parent_stmt;
DEALLOCATE PREPARE dict_parent_stmt;
START TRANSACTION;
INSERT IGNORE INTO sys_dict_type(type_code,name,description,sort_order)
VALUES('ECOMMERCE','电商大类','电商商家分类的根节点，下设食品类等子类型',10);
INSERT IGNORE INTO sys_dict_type(type_code,parent_type_code,name,description,sort_order)
VALUES('FOOD','ECOMMERCE','食品类','食品及餐饮商家，包含蛋糕店、餐厅、小吃店等',10);
INSERT IGNORE INTO sys_dict_item(type_code,item_code,name,description,sort_order) VALUES
('FOOD','CAKE_SHOP','蛋糕店','经营蛋糕、糕点等食品的商家',10),
('FOOD','RESTAURANT','餐厅','提供餐饮服务的商家',20),
('FOOD','SNACK_SHOP','小吃店','经营各类小吃的商家',30);
INSERT INTO showcase_shop_type(type_code,name,description,status,sort_order)
SELECT i.item_code,i.name,LEFT(i.description,300),
 CASE WHEN d.status='ENABLED' AND i.status='ENABLED' AND (d.parent_type_code IS NULL OR parent.status='ENABLED') THEN 'ENABLED' ELSE 'DISABLED' END,i.sort_order
FROM sys_dict_item i JOIN sys_dict_type d ON d.type_code=i.type_code
LEFT JOIN sys_dict_type parent ON parent.type_code=d.parent_type_code
WHERE i.type_code='MERCHANT_TYPE' OR d.parent_type_code='ECOMMERCE'
ON DUPLICATE KEY UPDATE name=VALUES(name),description=VALUES(description),status=VALUES(status),sort_order=VALUES(sort_order);
UPDATE showcase_product p JOIN showcase_shop_type t ON t.id=p.type_id
SET p.category=t.name,p.status=CASE WHEN t.status='DISABLED' AND p.status='PUBLISHED' THEN 'OFFLINE' ELSE p.status END;
COMMIT;
