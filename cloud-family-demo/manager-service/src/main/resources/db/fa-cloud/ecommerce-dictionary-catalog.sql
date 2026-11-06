USE `fa-cloud`;
-- 前置：ecommerce-dictionary-category.sql。只补齐缺失配置，保留已有状态、编码和商家/商品关联。
DROP TEMPORARY TABLE IF EXISTS ecommerce_category_seed;
CREATE TEMPORARY TABLE ecommerce_category_seed(type_code varchar(64) PRIMARY KEY,name varchar(100),sort_order int);
DROP TEMPORARY TABLE IF EXISTS ecommerce_merchant_seed;
CREATE TEMPORARY TABLE ecommerce_merchant_seed(type_code varchar(64),item_code varchar(64) PRIMARY KEY,name varchar(100),sort_order int);
INSERT INTO ecommerce_category_seed VALUES
('FOOD','食品餐饮',10),
('FRESH','生鲜农产',20),
('RETAIL','商超便利',30),
('FASHION','服饰鞋包',40),
('BEAUTY','美妆个护',50),
('HOME','家居日用',60),
('DIGITAL','数码家电',70),
('MATERNAL','母婴用品',80),
('SPORTS','运动户外',90),
('CULTURE','图书文具',100),
('PET','宠物用品',110),
('FLOWER_GIFT','鲜花礼品',120),
('AUTO','汽车用品',130);
INSERT INTO ecommerce_merchant_seed VALUES
('FOOD','CAKE_SHOP','蛋糕店',10),
('FOOD','RESTAURANT','餐厅',20),
('FOOD','SNACK_SHOP','小吃店',30),
('FOOD','BAKERY','烘焙店',40),
('FOOD','BEVERAGE_SHOP','饮品店',50),
('FOOD','DELI','熟食店',60),
('FRESH','FRUIT_STORE','水果店',10),
('FRESH','VEGETABLE_STORE','蔬菜店',20),
('FRESH','MEAT_POULTRY_STORE','肉禽店',30),
('FRESH','SEAFOOD_STORE','水产店',40),
('FRESH','GRAIN_OIL_STORE','粮油店',50),
('RETAIL','SUPERMARKET','超市',10),
('RETAIL','CONVENIENCE_STORE','便利店',20),
('RETAIL','DEPARTMENT_STORE','综合百货店',30),
('FASHION','WOMENSWEAR_STORE','女装店',10),
('FASHION','MENSWEAR_STORE','男装店',20),
('FASHION','CHILDRENSWEAR_STORE','童装店',30),
('FASHION','SHOE_STORE','鞋店',40),
('FASHION','BAG_STORE','箱包店',50),
('BEAUTY','COSMETICS_STORE','美妆店',10),
('BEAUTY','SKINCARE_STORE','护肤品店',20),
('BEAUTY','PERSONAL_CARE_STORE','个人护理用品店',30),
('HOME','FURNITURE_STORE','家具店',10),
('HOME','HOME_TEXTILE_STORE','家纺店',20),
('HOME','KITCHENWARE_STORE','厨具店',30),
('HOME','DAILY_GOODS_STORE','日用品店',40),
('HOME','HARDWARE_STORE','五金店',50),
('DIGITAL','PHONE_STORE','手机店',10),
('DIGITAL','COMPUTER_STORE','电脑店',20),
('DIGITAL','DIGITAL_ACCESSORIES_STORE','数码配件店',30),
('DIGITAL','APPLIANCE_STORE','家电店',40),
('MATERNAL','MATERNAL_STORE','母婴店',10),
('MATERNAL','BABY_GOODS_STORE','婴童用品店',20),
('MATERNAL','TOY_STORE','玩具店',30),
('SPORTS','SPORTING_GOODS_STORE','运动用品店',10),
('SPORTS','FITNESS_EQUIPMENT_STORE','健身器材店',20),
('SPORTS','OUTDOOR_STORE','户外用品店',30),
('CULTURE','BOOKSTORE','书店',10),
('CULTURE','STATIONERY_STORE','文具店',20),
('CULTURE','OFFICE_SUPPLIES_STORE','办公用品店',30),
('CULTURE','CULTURAL_CREATIVE_STORE','文创店',40),
('PET','PET_SUPPLIES_STORE','宠物用品店',10),
('PET','PET_FOOD_STORE','宠物食品店',20),
('FLOWER_GIFT','FLOWER_STORE','花店',10),
('FLOWER_GIFT','PLANT_STORE','绿植店',20),
('FLOWER_GIFT','GIFT_STORE','礼品店',30),
('FLOWER_GIFT','ACCESSORIES_STORE','饰品店',40),
('AUTO','AUTO_SUPPLIES_STORE','汽车用品店',10),
('AUTO','AUTO_PARTS_STORE','汽车配件店',20);
START TRANSACTION;
INSERT INTO sys_dict_type(type_code,parent_type_code,name,description,sort_order)
SELECT s.type_code,'ECOMMERCE',s.name,CONCAT(s.name,'类商家经营类型'),s.sort_order
FROM ecommerce_category_seed s WHERE NOT EXISTS(SELECT 1 FROM sys_dict_type d WHERE d.type_code=s.type_code);
-- 仅升级此前初始化的食品类名称，不覆盖用户自定义名称或状态。
UPDATE sys_dict_type SET name='食品餐饮' WHERE type_code='FOOD' AND parent_type_code='ECOMMERCE' AND name='食品类';
-- 分类迁移只调整所属字典，保留字典项ID、商家类型ID及所有业务关联。
UPDATE sys_dict_item i JOIN ecommerce_merchant_seed s ON (s.item_code=i.item_code OR s.name=i.name)
SET i.type_code=s.type_code WHERE i.type_code='MERCHANT_TYPE';
UPDATE sys_dict_item SET type_code='FRESH' WHERE type_code='MERCHANT_TYPE' AND item_code='FRUIT';
UPDATE sys_dict_item SET type_code='FOOD' WHERE type_code='MERCHANT_TYPE' AND item_code='CAKE';
UPDATE sys_dict_item SET name='超市' WHERE type_code='RETAIL' AND item_code='SUPERMARKET' AND name='market';
INSERT INTO sys_dict_item(type_code,item_code,name,description,sort_order)
SELECT s.type_code,s.item_code,s.name,CONCAT(s.name,'经营商家'),s.sort_order
FROM ecommerce_merchant_seed s
WHERE NOT EXISTS(SELECT 1 FROM sys_dict_item i JOIN sys_dict_type d ON d.type_code=i.type_code
 WHERE (i.type_code='MERCHANT_TYPE' OR d.parent_type_code='ECOMMERCE') AND (i.item_code=s.item_code OR i.name=s.name));
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
DROP TEMPORARY TABLE ecommerce_category_seed;
DROP TEMPORARY TABLE ecommerce_merchant_seed;
