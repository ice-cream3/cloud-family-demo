CREATE DATABASE IF NOT EXISTS `fa-model`
    DEFAULT CHARACTER SET utf8mb4
    DEFAULT COLLATE utf8mb4_0900_ai_ci;

USE `fa-model`;

CREATE TABLE IF NOT EXISTS partner_login_5m_stats (
    id bigint NOT NULL AUTO_INCREMENT,
    window_start_at timestamp NOT NULL,
    window_end_at timestamp NOT NULL,
    username varchar(200) NOT NULL,
    login_count bigint NOT NULL DEFAULT 0,
    first_login_at timestamp NULL DEFAULT NULL,
    last_login_at timestamp NULL DEFAULT NULL,
    calculated_at timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_partner_login_5m_stats_window_user (window_start_at, username),
    KEY idx_partner_login_5m_stats_window (window_start_at, window_end_at),
    KEY idx_partner_login_5m_stats_username (username)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
