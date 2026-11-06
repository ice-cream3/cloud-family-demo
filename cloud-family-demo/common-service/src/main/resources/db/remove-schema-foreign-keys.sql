-- 对当前选定业务库执行：mysql ... --database=fa-cloud < 本脚本
-- 先部署具备业务关联校验、锁和显式清理逻辑的 manager-service、partner-service。
-- 可重复执行；不删除数据、主键、唯一约束或索引。DDL会隐式提交。
DROP PROCEDURE IF EXISTS cloud_family_remove_foreign_keys;
DELIMITER $$
CREATE PROCEDURE cloud_family_remove_foreign_keys()
BEGIN
 DECLARE finished boolean DEFAULT FALSE;
 DECLARE child_table varchar(64);
 DECLARE fk_name varchar(64);
 DECLARE foreign_keys CURSOR FOR
  SELECT table_name,constraint_name FROM information_schema.referential_constraints
  WHERE constraint_schema=DATABASE() ORDER BY table_name,constraint_name;
 DECLARE CONTINUE HANDLER FOR NOT FOUND SET finished=TRUE;
 IF DATABASE() IS NULL OR DATABASE() NOT IN ('fa-cloud','fa-model','fa-cloud-test','fa-model-test') THEN
  SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Select a cloud-family-demo business schema first';
 END IF;
 OPEN foreign_keys;
 drop_loop: LOOP
  FETCH foreign_keys INTO child_table,fk_name;
  IF finished THEN LEAVE drop_loop; END IF;
  SET @drop_fk_ddl=CONCAT('ALTER TABLE `',REPLACE(DATABASE(),'`','``'),'`.`',REPLACE(child_table,'`','``'),'` DROP FOREIGN KEY `',REPLACE(fk_name,'`','``'),'`');
  PREPARE drop_fk_stmt FROM @drop_fk_ddl;
  EXECUTE drop_fk_stmt;
  DEALLOCATE PREPARE drop_fk_stmt;
 END LOOP;
 CLOSE foreign_keys;
END$$
DELIMITER ;
CALL cloud_family_remove_foreign_keys();
DROP PROCEDURE cloud_family_remove_foreign_keys;
SELECT DATABASE() AS schema_name,COUNT(*) AS remaining_foreign_keys FROM information_schema.referential_constraints
WHERE constraint_schema=DATABASE();
