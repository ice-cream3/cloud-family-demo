USE `fa-cloud`;

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
