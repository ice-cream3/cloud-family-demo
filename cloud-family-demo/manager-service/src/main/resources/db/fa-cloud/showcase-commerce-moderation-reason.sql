USE `fa-cloud`;

-- 兼容已初始化的数据库，可重复执行。
SELECT IF(COUNT(*)=0,
 'ALTER TABLE showcase_product ADD COLUMN moderation_reason varchar(500) DEFAULT NULL COMMENT ''系统停用原因，商家只读'' AFTER moderation_status',
 'SELECT 1') INTO @showcase_reason_ddl
FROM information_schema.columns
WHERE table_schema=DATABASE() AND table_name='showcase_product' AND column_name='moderation_reason';
PREPARE showcase_reason_stmt FROM @showcase_reason_ddl;
EXECUTE showcase_reason_stmt;
DEALLOCATE PREPARE showcase_reason_stmt;

UPDATE showcase_product SET moderation_reason='历史停用记录，具体原因请联系管理员'
WHERE moderation_status='BLOCKED' AND (moderation_reason IS NULL OR TRIM(moderation_reason)='');
