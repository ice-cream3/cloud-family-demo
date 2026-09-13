CREATE DATABASE IF NOT EXISTS `fa-cloud`
    DEFAULT CHARACTER SET utf8mb4
    DEFAULT COLLATE utf8mb4_0900_ai_ci;

USE `fa-cloud`;

CREATE TABLE IF NOT EXISTS partner_login_log (
    id bigint NOT NULL AUTO_INCREMENT,
    username varchar(64) NOT NULL,
    user_type varchar(32) NOT NULL,
    client_id varchar(100) NOT NULL,
    grant_type varchar(100) NOT NULL,
    client_ip varchar(64) DEFAULT NULL,
    user_agent varchar(512) DEFAULT NULL,
    device_type varchar(32) DEFAULT NULL,
    browser varchar(64) DEFAULT NULL,
    operating_system varchar(64) DEFAULT NULL,
    login_at timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
    login_result varchar(32) NOT NULL,
    failure_reason varchar(128) DEFAULT NULL,
    created_at timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_partner_login_log_username_login_at (username, login_at),
    KEY idx_partner_login_log_user_type_login_at (user_type, login_at),
    KEY idx_partner_login_log_client_ip_login_at (client_ip, login_at),
    KEY idx_partner_login_log_result_login_at (login_result, login_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS manager_login_log (
    id bigint NOT NULL AUTO_INCREMENT,
    username varchar(64) NOT NULL,
    user_type varchar(32) NOT NULL,
    client_id varchar(100) NOT NULL,
    grant_type varchar(100) NOT NULL,
    client_ip varchar(64) DEFAULT NULL,
    user_agent varchar(512) DEFAULT NULL,
    device_type varchar(32) DEFAULT NULL,
    browser varchar(64) DEFAULT NULL,
    operating_system varchar(64) DEFAULT NULL,
    login_at timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
    login_result varchar(32) NOT NULL,
    failure_reason varchar(128) DEFAULT NULL,
    created_at timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_manager_login_log_username_login_at (username, login_at),
    KEY idx_manager_login_log_user_type_login_at (user_type, login_at),
    KEY idx_manager_login_log_client_ip_login_at (client_ip, login_at),
    KEY idx_manager_login_log_result_login_at (login_result, login_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
