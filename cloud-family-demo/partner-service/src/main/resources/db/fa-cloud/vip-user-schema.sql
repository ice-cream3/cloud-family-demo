CREATE DATABASE IF NOT EXISTS `fa-cloud`
    DEFAULT CHARACTER SET utf8mb4
    DEFAULT COLLATE utf8mb4_0900_ai_ci;

USE `fa-cloud`;

CREATE TABLE IF NOT EXISTS vip_user (
    id bigint NOT NULL AUTO_INCREMENT,
    username varchar(64) NOT NULL,
    password_hash varchar(255) DEFAULT NULL,
    display_name varchar(100) NOT NULL,
    email varchar(255) DEFAULT NULL,
    phone varchar(32) DEFAULT NULL,
    vip_level varchar(32) NOT NULL DEFAULT 'NORMAL',
    status varchar(32) NOT NULL DEFAULT 'ENABLED',
    created_at timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_vip_user_username (username),
    KEY idx_vip_user_status (status),
    KEY idx_vip_user_vip_level (vip_level)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

INSERT IGNORE INTO vip_user (username, password_hash, display_name, email, phone, vip_level, status)
VALUES
    ('alice', 'demo', 'API Demo User', 'alice@example.com', NULL, 'NORMAL', 'ENABLED');
