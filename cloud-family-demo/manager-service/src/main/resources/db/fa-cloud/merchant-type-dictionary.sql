USE `fa-cloud`;
-- 在 system-dictionary.sql 和 showcase-shop-types.sql 后执行；可重复执行。
START TRANSACTION;
INSERT IGNORE INTO sys_dict_type(type_code,name,description)
VALUES('MERCHANT_TYPE','商家类型','商家经营范围及商品类型的唯一维护入口');
-- 保留历史编码、状态及关联 ID，避免已有商家和商品丢失类型。
INSERT IGNORE INTO sys_dict_item(type_code,item_code,name,description,status,sort_order)
SELECT 'MERCHANT_TYPE',type_code,name,description,status,sort_order FROM showcase_shop_type;
INSERT INTO sys_dict_item(type_code,item_code,name,sort_order)
SELECT 'MERCHANT_TYPE','FRUIT_STORE','水果店',10
WHERE NOT EXISTS(SELECT 1 FROM sys_dict_item WHERE type_code='MERCHANT_TYPE' AND (item_code='FRUIT_STORE' OR name='水果店'));
INSERT INTO sys_dict_item(type_code,item_code,name,sort_order)
SELECT 'MERCHANT_TYPE','SUPERMARKET','超市',20
WHERE NOT EXISTS(SELECT 1 FROM sys_dict_item WHERE type_code='MERCHANT_TYPE' AND (item_code='SUPERMARKET' OR name='超市'));
INSERT INTO showcase_shop_type(type_code,name,description,status,sort_order)
SELECT i.item_code,i.name,LEFT(i.description,300),CASE WHEN d.status='ENABLED' AND i.status='ENABLED' THEN 'ENABLED' ELSE 'DISABLED' END,i.sort_order
FROM sys_dict_item i JOIN sys_dict_type d ON d.type_code=i.type_code WHERE i.type_code='MERCHANT_TYPE'
ON DUPLICATE KEY UPDATE name=VALUES(name),description=VALUES(description),status=VALUES(status),sort_order=VALUES(sort_order);
UPDATE showcase_product p JOIN showcase_shop_type t ON t.id=p.type_id
SET p.category=t.name,p.status=CASE WHEN t.status='DISABLED' AND p.status='PUBLISHED' THEN 'OFFLINE' ELSE p.status END;
COMMIT;
