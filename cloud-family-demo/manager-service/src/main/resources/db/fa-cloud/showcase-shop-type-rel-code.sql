USE `fa-cloud`;

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

-- 兼容旧库：按实际元数据移除类型ID关联外键；新库不创建外键。
SELECT IF(COUNT(*)=0,'SELECT 1',CONCAT('ALTER TABLE showcase_shop_type_rel ',
 GROUP_CONCAT(CONCAT('DROP FOREIGN KEY `',REPLACE(constraint_name,'`','``'),'`') SEPARATOR ', '))) INTO @showcase_rel_fk_drop
FROM information_schema.key_column_usage
WHERE table_schema=DATABASE() AND table_name='showcase_shop_type_rel' AND column_name='type_id' AND referenced_table_name IS NOT NULL;
PREPARE showcase_rel_fk_drop_stmt FROM @showcase_rel_fk_drop;
EXECUTE showcase_rel_fk_drop_stmt;
DEALLOCATE PREPARE showcase_rel_fk_drop_stmt;

SELECT IF(COUNT(*)>0,
 'ALTER TABLE showcase_shop_type_rel DROP PRIMARY KEY, DROP INDEX idx_showcase_type_shop, DROP COLUMN type_id, MODIFY COLUMN type_code varchar(64) NOT NULL, ADD PRIMARY KEY(shop_id,type_code), ADD KEY idx_showcase_type_shop(type_code,shop_id)',
 'SELECT 1') INTO @showcase_rel_code_replace
FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='showcase_shop_type_rel' AND column_name='type_id';
PREPARE showcase_rel_code_replace_stmt FROM @showcase_rel_code_replace;
EXECUTE showcase_rel_code_replace_stmt;
DEALLOCATE PREPARE showcase_rel_code_replace_stmt;
