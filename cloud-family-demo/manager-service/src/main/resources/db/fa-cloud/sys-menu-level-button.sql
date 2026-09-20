USE `fa-cloud`;

DROP PROCEDURE IF EXISTS add_sys_menu_level_button_columns;

DELIMITER //
CREATE PROCEDURE add_sys_menu_level_button_columns()
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_schema = DATABASE()
          AND table_name = 'sys_menu'
          AND column_name = 'menu_level'
    ) THEN
        ALTER TABLE sys_menu ADD COLUMN menu_level int NOT NULL DEFAULT 1 AFTER icon;
    END IF;

    IF NOT EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_schema = DATABASE()
          AND table_name = 'sys_menu'
          AND column_name = 'button_flag'
    ) THEN
        ALTER TABLE sys_menu ADD COLUMN button_flag tinyint(1) NOT NULL DEFAULT 0 AFTER menu_level;
    END IF;
END//
DELIMITER ;

CALL add_sys_menu_level_button_columns();

DROP PROCEDURE IF EXISTS add_sys_menu_level_button_columns;

UPDATE sys_menu
SET button_flag = 1
WHERE component = 'ButtonPermission';

UPDATE sys_menu
SET menu_level = 1
WHERE parent_id IS NULL;

UPDATE sys_menu child
INNER JOIN sys_menu parent ON parent.id = child.parent_id
SET child.menu_level = parent.menu_level + 1
WHERE child.parent_id IS NOT NULL;

UPDATE sys_menu child
INNER JOIN sys_menu parent ON parent.id = child.parent_id
SET child.menu_level = parent.menu_level + 1
WHERE child.parent_id IS NOT NULL;
