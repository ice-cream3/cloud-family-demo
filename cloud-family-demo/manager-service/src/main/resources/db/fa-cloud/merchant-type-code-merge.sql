USE `fa-cloud`;
-- 前置：ecommerce-dictionary-catalog.sql；合并旧商家类型，保留目标类型ID及业务记录。
-- CAKE -> CAKE_SHOP，FRUIT -> FRUIT_STORE。可重复执行，通过目标存在性检查保护业务关联。
DROP TEMPORARY TABLE IF EXISTS merchant_type_code_map;
CREATE TEMPORARY TABLE merchant_type_code_map (
 old_code varchar(64) PRIMARY KEY,
 new_code varchar(64) NOT NULL,
 dict_type_code varchar(64) NOT NULL
);
INSERT INTO merchant_type_code_map VALUES ('CAKE','CAKE_SHOP','FOOD'),('FRUIT','FRUIT_STORE','FRESH');
DROP TEMPORARY TABLE IF EXISTS merchant_type_merge_guard;
CREATE TEMPORARY TABLE merchant_type_merge_guard (
 target_count int NOT NULL CHECK(target_count=2)
);
START TRANSACTION;
-- 锁定待迁移类型，确保迁移期间目标不被删除或改名。
SELECT t.id,t.type_code FROM showcase_shop_type t
JOIN merchant_type_code_map m ON t.type_code=m.old_code OR t.type_code=m.new_code
ORDER BY t.id FOR UPDATE;
-- 缺少目标类型/字典项时立即报错，连接终止时事务回滚。
INSERT INTO merchant_type_merge_guard
SELECT COUNT(*) FROM merchant_type_code_map m
JOIN showcase_shop_type t ON t.type_code=m.new_code
JOIN sys_dict_item i ON i.type_code=m.dict_type_code AND i.item_code=m.new_code;
-- 先补目标关联；同时经营新旧类型的店铺自动合并为一个关联。
INSERT INTO showcase_shop_type_rel(shop_id,type_code)
SELECT r.shop_id,m.new_code FROM showcase_shop_type_rel r
JOIN merchant_type_code_map m ON m.old_code=r.type_code
WHERE NOT EXISTS(SELECT 1 FROM showcase_shop_type_rel existing WHERE existing.shop_id=r.shop_id AND existing.type_code=m.new_code);
UPDATE showcase_product p
JOIN showcase_shop_type old_type ON old_type.id=p.type_id
JOIN merchant_type_code_map m ON m.old_code=old_type.type_code
JOIN showcase_shop_type new_type ON new_type.type_code=m.new_code
SET p.type_id=new_type.id,p.category=new_type.name;
DELETE r FROM showcase_shop_type_rel r JOIN merchant_type_code_map m ON r.type_code=m.old_code;
DELETE t FROM showcase_shop_type t JOIN merchant_type_code_map m ON t.type_code=m.old_code;
DELETE i FROM sys_dict_item i JOIN merchant_type_code_map m ON i.item_code=m.old_code
JOIN sys_dict_type d ON d.type_code=i.type_code
WHERE d.parent_type_code='ECOMMERCE' OR d.type_code='MERCHANT_TYPE';
COMMIT;
DROP TEMPORARY TABLE merchant_type_merge_guard;
DROP TEMPORARY TABLE merchant_type_code_map;
