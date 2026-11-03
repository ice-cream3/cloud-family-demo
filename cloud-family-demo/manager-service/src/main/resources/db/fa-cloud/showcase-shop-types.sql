USE `fa-cloud`;

CREATE TABLE IF NOT EXISTS showcase_shop_type (
 id bigint NOT NULL AUTO_INCREMENT COMMENT '商家/商品类型ID',
 type_code varchar(64) NOT NULL COMMENT '唯一业务编码',
 name varchar(32) NOT NULL COMMENT '类型名称，仅展示',
 description varchar(300) DEFAULT NULL,
 status varchar(16) NOT NULL DEFAULT 'ENABLED' COMMENT 'ENABLED启用/DISABLED停用',
 sort_order int NOT NULL DEFAULT 0,
 created_at timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
 updated_at timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
 PRIMARY KEY(id),UNIQUE KEY uk_showcase_type_code(type_code),KEY idx_showcase_type_status_sort(status,sort_order,id),
 CONSTRAINT chk_showcase_type_status CHECK(status IN ('ENABLED','DISABLED'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='商家类型字典，同时约束商品类型';


-- 先增加可空字段，按原ID生成稳定业务编码，再添加非空与唯一约束。
SELECT IF(COUNT(*)=0,
 'ALTER TABLE showcase_shop_type ADD COLUMN type_code varchar(64) DEFAULT NULL COMMENT ''唯一业务编码，创建后不可修改'' AFTER id',
 'SELECT 1') INTO @showcase_type_code_ddl
FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='showcase_shop_type' AND column_name='type_code';
PREPARE showcase_type_code_stmt FROM @showcase_type_code_ddl;
EXECUTE showcase_type_code_stmt;
DEALLOCATE PREPARE showcase_type_code_stmt;
UPDATE showcase_shop_type SET type_code=CONCAT('TYPE_',id) WHERE type_code IS NULL OR TRIM(type_code)='';
ALTER TABLE showcase_shop_type MODIFY COLUMN type_code varchar(64) NOT NULL COMMENT '唯一业务编码，创建后不可修改';
SELECT IF(COUNT(*)=0,'ALTER TABLE showcase_shop_type ADD UNIQUE KEY uk_showcase_type_code(type_code)','SELECT 1') INTO @showcase_type_code_index
FROM information_schema.statistics WHERE table_schema=DATABASE() AND table_name='showcase_shop_type' AND index_name='uk_showcase_type_code';
PREPARE showcase_type_code_index_stmt FROM @showcase_type_code_index;
EXECUTE showcase_type_code_index_stmt;
DEALLOCATE PREPARE showcase_type_code_index_stmt;
-- 名称仅用于展示，允许重名。
SELECT IF(COUNT(*)>0,'ALTER TABLE showcase_shop_type DROP INDEX uk_showcase_type_name','SELECT 1') INTO @showcase_type_name_index
FROM information_schema.statistics WHERE table_schema=DATABASE() AND table_name='showcase_shop_type' AND index_name='uk_showcase_type_name';
PREPARE showcase_type_name_index_stmt FROM @showcase_type_name_index;
EXECUTE showcase_type_name_index_stmt;
DEALLOCATE PREPARE showcase_type_name_index_stmt;

CREATE TABLE IF NOT EXISTS showcase_shop_type_rel (
 shop_id bigint NOT NULL,type_code varchar(64) NOT NULL,
 PRIMARY KEY(shop_id,type_code),KEY idx_showcase_type_shop(type_code,shop_id),
 CONSTRAINT fk_showcase_rel_shop FOREIGN KEY(shop_id) REFERENCES showcase_shop(id) ON DELETE CASCADE,
 CONSTRAINT fk_showcase_rel_type_code FOREIGN KEY(type_code) REFERENCES showcase_shop_type(type_code) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='商家允许经营的类型，多选';


-- 商家类型表的 type_code 必须已由 showcase-shop-type-code.sql 初始化。
-- 先回填编码，再一次性替换旧外键、主键和列，保留所有现有关联。
SELECT IF(COUNT(*)=0,
 'ALTER TABLE showcase_shop_type_rel ADD COLUMN type_code varchar(64) DEFAULT NULL AFTER shop_id',
 'SELECT 1') INTO @showcase_rel_code_add
FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='showcase_shop_type_rel' AND column_name='type_code';
PREPARE showcase_rel_code_add_stmt FROM @showcase_rel_code_add;
EXECUTE showcase_rel_code_add_stmt;
DEALLOCATE PREPARE showcase_rel_code_add_stmt;

SELECT IF(COUNT(*)>0,
 'UPDATE showcase_shop_type_rel r JOIN showcase_shop_type t ON t.id=r.type_id SET r.type_code=t.type_code',
 'SELECT 1') INTO @showcase_rel_code_fill
FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='showcase_shop_type_rel' AND column_name='type_id';
PREPARE showcase_rel_code_fill_stmt FROM @showcase_rel_code_fill;
EXECUTE showcase_rel_code_fill_stmt;
DEALLOCATE PREPARE showcase_rel_code_fill_stmt;

SELECT IF(COUNT(*)>0,
 'ALTER TABLE showcase_shop_type_rel DROP FOREIGN KEY fk_showcase_rel_type, DROP PRIMARY KEY, DROP INDEX idx_showcase_type_shop, DROP COLUMN type_id, MODIFY COLUMN type_code varchar(64) NOT NULL, ADD PRIMARY KEY(shop_id,type_code), ADD KEY idx_showcase_type_shop(type_code,shop_id), ADD CONSTRAINT fk_showcase_rel_type_code FOREIGN KEY(type_code) REFERENCES showcase_shop_type(type_code) ON DELETE RESTRICT ON UPDATE RESTRICT',
 'SELECT 1') INTO @showcase_rel_code_replace
FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='showcase_shop_type_rel' AND column_name='type_id';
PREPARE showcase_rel_code_replace_stmt FROM @showcase_rel_code_replace;
EXECUTE showcase_rel_code_replace_stmt;
DEALLOCATE PREPARE showcase_rel_code_replace_stmt;

SELECT IF(COUNT(*)=0,
 'ALTER TABLE showcase_product ADD COLUMN type_id bigint DEFAULT NULL COMMENT ''商品类型ID'' AFTER shop_id, ADD KEY idx_showcase_product_type(type_id), ADD CONSTRAINT fk_showcase_product_type FOREIGN KEY(type_id) REFERENCES showcase_shop_type(id) ON DELETE RESTRICT',
 'SELECT 1') INTO @showcase_product_type_ddl
FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='showcase_product' AND column_name='type_id';
PREPARE showcase_product_type_stmt FROM @showcase_product_type_ddl;
EXECUTE showcase_product_type_stmt;
DEALLOCATE PREPARE showcase_product_type_stmt;

-- 兼容旧数据：保留原分类名称；只对尚未迁移的商品/商家补关联，重复执行不覆盖已配置类型。
INSERT IGNORE INTO showcase_shop_type(type_code,name,description)
SELECT DISTINCT CONCAT('LEGACY_',UPPER(LEFT(SHA2(COALESCE(NULLIF(TRIM(category),''),'综合'),256),32))), COALESCE(NULLIF(TRIM(category),''),'综合'),'从历史商品分类迁移' FROM showcase_product WHERE type_id IS NULL;
INSERT IGNORE INTO showcase_shop_type(type_code,name,description) VALUES('GENERAL','综合','历史空店铺默认类型，可在店铺资料中修改');
INSERT IGNORE INTO showcase_shop_type_rel(shop_id,type_code)
SELECT DISTINCT p.shop_id,t.type_code FROM showcase_product p JOIN showcase_shop_type t ON t.type_code=CONCAT('LEGACY_',UPPER(LEFT(SHA2(COALESCE(NULLIF(TRIM(p.category),''),'综合'),256),32))) WHERE p.type_id IS NULL;
UPDATE showcase_product p JOIN showcase_shop_type t ON t.type_code=CONCAT('LEGACY_',UPPER(LEFT(SHA2(COALESCE(NULLIF(TRIM(p.category),''),'综合'),256),32))) SET p.type_id=t.id,p.category=t.name WHERE p.type_id IS NULL;
INSERT IGNORE INTO showcase_shop_type_rel(shop_id,type_code)
SELECT s.id,t.type_code FROM showcase_shop s JOIN showcase_shop_type t ON t.type_code='GENERAL' WHERE NOT EXISTS(SELECT 1 FROM showcase_shop_type_rel r WHERE r.shop_id=s.id);
