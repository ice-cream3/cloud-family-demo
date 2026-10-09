USE `fa-cloud`;

CREATE TABLE IF NOT EXISTS user_notification_setting (
    id bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    username varchar(64) NOT NULL COMMENT '用户账号',
    system_enabled tinyint(1) NOT NULL DEFAULT 1 COMMENT '系统通知开关',
    activity_enabled tinyint(1) NOT NULL DEFAULT 1 COMMENT '活动通知开关',
    task_enabled tinyint(1) NOT NULL DEFAULT 0 COMMENT '任务通知开关',
    updated_at timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_user_notification_username (username)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='用户通知设置';

CREATE TABLE IF NOT EXISTS app_document (
    id bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    document_type varchar(32) NOT NULL COMMENT '文档类型',
    title varchar(100) NOT NULL COMMENT '文档标题',
    content text NOT NULL COMMENT '文档内容',
    updated_at timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_app_document_type (document_type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='应用协议文档';

CREATE TABLE IF NOT EXISTS app_version (
    id bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    version_name varchar(32) NOT NULL COMMENT '版本号',
    latest tinyint(1) NOT NULL DEFAULT 1 COMMENT '是否最新版本',
    release_note varchar(500) DEFAULT NULL COMMENT '更新说明',
    created_at timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_app_version_name (version_name),
    KEY idx_app_version_latest (latest)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='应用版本信息';

CREATE TABLE IF NOT EXISTS user_membership (
    id bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    username varchar(64) NOT NULL COMMENT '用户账号',
    plan_name varchar(64) NOT NULL DEFAULT '普通会员' COMMENT '会员方案名称',
    status varchar(32) NOT NULL DEFAULT 'ACTIVE' COMMENT '会员状态',
    expire_at timestamp NULL DEFAULT NULL COMMENT '到期时间',
    benefits varchar(500) DEFAULT NULL COMMENT '会员权益说明',
    updated_at timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_user_membership_username (username)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='用户会员信息';

CREATE TABLE IF NOT EXISTS user_history (
    id bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    username varchar(64) NOT NULL COMMENT '用户账号',
    category varchar(32) NOT NULL COMMENT '历史分类',
    title varchar(100) NOT NULL COMMENT '历史标题',
    description varchar(255) DEFAULT NULL COMMENT '历史描述',
    icon_tone varchar(32) DEFAULT NULL COMMENT '图标色调',
    occurred_at timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '发生时间',
    PRIMARY KEY (id),
    KEY idx_user_history_username_time (username, occurred_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='用户历史记录';

CREATE TABLE IF NOT EXISTS user_favorite (
    id bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    username varchar(64) NOT NULL COMMENT '用户账号',
    item_type varchar(32) NOT NULL COMMENT '收藏对象类型',
    title varchar(100) NOT NULL COMMENT '收藏标题',
    description varchar(255) DEFAULT NULL COMMENT '收藏描述',
    icon_tone varchar(32) DEFAULT NULL COMMENT '图标色调',
    created_at timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_user_favorite_item (username, item_type, title),
    KEY idx_user_favorite_username_time (username, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='用户收藏';

CREATE TABLE IF NOT EXISTS user_trip_slot (
    id bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    owner_username varchar(64) NOT NULL COMMENT '发布人账号',
    owner_display_name varchar(100) NOT NULL COMMENT '发布人显示名称',
    title varchar(100) NOT NULL COMMENT '行程标题',
    place varchar(160) NOT NULL COMMENT '行程地点',
    trip_date date NOT NULL COMMENT '行程日期',
    start_time time NOT NULL COMMENT '开始时间',
    end_time time NOT NULL COMMENT '结束时间',
    status varchar(32) NOT NULL DEFAULT 'OPEN' COMMENT '行程状态',
    applicant_username varchar(64) DEFAULT NULL COMMENT '预约人账号',
    applicant_display_name varchar(100) DEFAULT NULL COMMENT '预约人显示名称',
    apply_note varchar(255) DEFAULT NULL COMMENT '预约申请备注',
    applied_at timestamp NULL DEFAULT NULL COMMENT '预约申请时间',
    reviewed_at timestamp NULL DEFAULT NULL COMMENT '审核时间',
    created_at timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_user_trip_owner_slot (owner_username, trip_date, start_time, end_time),
    KEY idx_user_trip_owner_date (owner_username, trip_date, start_time),
    KEY idx_user_trip_status_date (status, trip_date, start_time),
    KEY idx_user_trip_applicant (applicant_username, status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='用户行程预约时段';

DROP PROCEDURE IF EXISTS ensure_user_profile_id_primary_key;

DELIMITER //

CREATE PROCEDURE ensure_user_profile_id_primary_key(
    IN target_table varchar(64),
    IN natural_key_column varchar(64),
    IN natural_unique_key varchar(64)
)
BEGIN
    DECLARE id_column_count int DEFAULT 0;
    DECLARE primary_key_count int DEFAULT 0;
    DECLARE natural_unique_count int DEFAULT 0;

    SELECT COUNT(*)
    INTO id_column_count
    FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = target_table
      AND column_name = 'id';

    IF id_column_count = 0 THEN
        SELECT COUNT(*)
        INTO primary_key_count
        FROM information_schema.table_constraints
        WHERE table_schema = DATABASE()
          AND table_name = target_table
          AND constraint_type = 'PRIMARY KEY';

        IF primary_key_count > 0 THEN
            SET @drop_primary_sql = CONCAT('ALTER TABLE ', target_table, ' DROP PRIMARY KEY');
            PREPARE drop_primary_stmt FROM @drop_primary_sql;
            EXECUTE drop_primary_stmt;
            DEALLOCATE PREPARE drop_primary_stmt;
        END IF;

        SET @add_id_sql = CONCAT('ALTER TABLE ', target_table, ' ADD COLUMN id bigint NOT NULL AUTO_INCREMENT FIRST, ADD PRIMARY KEY (id)');
        PREPARE add_id_stmt FROM @add_id_sql;
        EXECUTE add_id_stmt;
        DEALLOCATE PREPARE add_id_stmt;

        SELECT COUNT(*)
        INTO natural_unique_count
        FROM information_schema.statistics
        WHERE table_schema = DATABASE()
          AND table_name = target_table
          AND index_name = natural_unique_key;

        IF natural_unique_count = 0 THEN
            SET @add_unique_sql = CONCAT('ALTER TABLE ', target_table, ' ADD UNIQUE KEY ', natural_unique_key, ' (', natural_key_column, ')');
            PREPARE add_unique_stmt FROM @add_unique_sql;
            EXECUTE add_unique_stmt;
            DEALLOCATE PREPARE add_unique_stmt;
        END IF;
    END IF;
END//

DELIMITER ;

CALL ensure_user_profile_id_primary_key('user_notification_setting', 'username', 'uk_user_notification_username');
CALL ensure_user_profile_id_primary_key('app_document', 'document_type', 'uk_app_document_type');
CALL ensure_user_profile_id_primary_key('user_membership', 'username', 'uk_user_membership_username');

DROP PROCEDURE IF EXISTS ensure_user_profile_id_primary_key;

INSERT INTO app_document (document_type, title, content)
VALUES
    ('about', 'Cloud Family Tools', '一站式移动工具箱，提供图片处理、文件转换、扫描识别和生活工具。当前演示版本用于展示移动端工作流与用户中心能力。'),
    ('privacy', '隐私政策摘要', '我们仅在提供登录、工具处理和账号服务时使用必要信息。未经授权不会向第三方出售或共享你的个人信息。'),
    ('agreement', '用户协议摘要', '使用本服务即表示你同意遵守平台规则，不上传违法或侵权内容。部分工具能力可能受网络、文件格式和服务状态影响。')
ON DUPLICATE KEY UPDATE
    title = VALUES(title),
    content = VALUES(content);

INSERT INTO app_version (version_name, latest, release_note)
VALUES ('v1.2.0', 1, '当前已是最新版本')
ON DUPLICATE KEY UPDATE
    latest = VALUES(latest),
    release_note = VALUES(release_note);

INSERT IGNORE INTO user_membership (username, plan_name, status, benefits)
SELECT username, '普通会员', 'ACTIVE', '基础工具可用'
FROM vip_user;

INSERT IGNORE INTO user_history (username, category, title, description, icon_tone)
SELECT username, '图片处理', '图片压缩', '3张图片 · 2.4MB -> 856KB', 'green'
FROM vip_user;

INSERT IGNORE INTO user_history (username, category, title, description, icon_tone)
SELECT username, '文件转换', '图片转PDF', '5张图片', 'orange'
FROM vip_user;

INSERT IGNORE INTO user_favorite (username, item_type, title, description, icon_tone)
SELECT username, 'tool', '图片压缩', '常用图片处理工具', 'green'
FROM vip_user;

INSERT INTO user_trip_slot (owner_username, owner_display_name, title, place, trip_date, start_time, end_time, status)
SELECT username, display_name, '产品方案沟通', '线上会议', DATE_ADD(CURRENT_DATE, INTERVAL 1 DAY), '10:00:00', '11:00:00', 'OPEN'
FROM vip_user
WHERE username = 'superman'
  AND NOT EXISTS (
      SELECT 1
      FROM user_trip_slot
      WHERE owner_username = vip_user.username
        AND title = '产品方案沟通'
        AND trip_date = DATE_ADD(CURRENT_DATE, INTERVAL 1 DAY)
        AND start_time = '10:00:00'
  );

INSERT INTO user_trip_slot (owner_username, owner_display_name, title, place, trip_date, start_time, end_time, status, applicant_username, applicant_display_name, apply_note, applied_at)
SELECT username, display_name, '线下咖啡交流', '创意园咖啡厅', DATE_ADD(CURRENT_DATE, INTERVAL 2 DAY), '15:30:00', '16:30:00', 'PENDING', 'demo_b', 'B 用户', '想聊一下合作排期', CURRENT_TIMESTAMP
FROM vip_user
WHERE username = 'superman'
  AND NOT EXISTS (
      SELECT 1
      FROM user_trip_slot
      WHERE owner_username = vip_user.username
        AND title = '线下咖啡交流'
        AND trip_date = DATE_ADD(CURRENT_DATE, INTERVAL 2 DAY)
        AND start_time = '15:30:00'
  );
