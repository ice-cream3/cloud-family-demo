CREATE DATABASE IF NOT EXISTS `fa-cloud`
    DEFAULT CHARACTER SET utf8mb4
    DEFAULT COLLATE utf8mb4_0900_ai_ci;

USE `fa-cloud`;

CREATE TABLE IF NOT EXISTS sys_user (
    id bigint NOT NULL AUTO_INCREMENT,
    username varchar(64) NOT NULL,
    password_hash varchar(255) DEFAULT NULL,
    display_name varchar(100) NOT NULL,
    email varchar(255) DEFAULT NULL,
    status varchar(32) NOT NULL DEFAULT 'ENABLED',
    created_at timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_sys_user_username (username),
    KEY idx_sys_user_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS sys_role (
    id bigint NOT NULL AUTO_INCREMENT,
    role_code varchar(64) NOT NULL,
    role_name varchar(100) NOT NULL,
    description varchar(500) DEFAULT NULL,
    status varchar(32) NOT NULL DEFAULT 'ENABLED',
    created_at timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_sys_role_role_code (role_code),
    KEY idx_sys_role_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS sys_permission (
    id bigint NOT NULL AUTO_INCREMENT,
    permission_code varchar(128) NOT NULL,
    permission_name varchar(100) NOT NULL,
    description varchar(500) DEFAULT NULL,
    status varchar(32) NOT NULL DEFAULT 'ENABLED',
    created_at timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_sys_permission_permission_code (permission_code),
    KEY idx_sys_permission_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS sys_menu (
    id bigint NOT NULL AUTO_INCREMENT,
    parent_id bigint DEFAULT NULL,
    menu_code varchar(64) NOT NULL,
    menu_name varchar(100) NOT NULL,
    path varchar(255) DEFAULT NULL,
    component varchar(255) DEFAULT NULL,
    icon varchar(100) DEFAULT NULL,
    sort_order int NOT NULL DEFAULT 0,
    visible tinyint(1) NOT NULL DEFAULT 1,
    status varchar(32) NOT NULL DEFAULT 'ENABLED',
    created_at timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_sys_menu_menu_code (menu_code),
    KEY idx_sys_menu_parent_id (parent_id),
    KEY idx_sys_menu_status (status),
    CONSTRAINT fk_sys_menu_parent FOREIGN KEY (parent_id) REFERENCES sys_menu (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS sys_user_role (
    user_id bigint NOT NULL,
    role_id bigint NOT NULL,
    created_at timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (user_id, role_id),
    CONSTRAINT fk_sys_user_role_user FOREIGN KEY (user_id) REFERENCES sys_user (id) ON DELETE CASCADE,
    CONSTRAINT fk_sys_user_role_role FOREIGN KEY (role_id) REFERENCES sys_role (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS sys_role_permission (
    role_id bigint NOT NULL,
    permission_id bigint NOT NULL,
    created_at timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (role_id, permission_id),
    CONSTRAINT fk_sys_role_permission_role FOREIGN KEY (role_id) REFERENCES sys_role (id) ON DELETE CASCADE,
    CONSTRAINT fk_sys_role_permission_permission FOREIGN KEY (permission_id) REFERENCES sys_permission (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS sys_role_menu (
    role_id bigint NOT NULL,
    menu_id bigint NOT NULL,
    created_at timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (role_id, menu_id),
    CONSTRAINT fk_sys_role_menu_role FOREIGN KEY (role_id) REFERENCES sys_role (id) ON DELETE CASCADE,
    CONSTRAINT fk_sys_role_menu_menu FOREIGN KEY (menu_id) REFERENCES sys_menu (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

INSERT IGNORE INTO sys_user (username, password_hash, display_name, email, status)
VALUES
    ('alice', 'demo', 'API Demo User', 'alice@example.com', 'ENABLED'),
    ('manager', 'demo', 'Manager Demo User', 'manager@example.com', 'ENABLED'),
    ('admin', 'demo', 'Admin User', 'admin@example.com', 'ENABLED');

INSERT IGNORE INTO sys_role (role_code, role_name, description, status)
VALUES
    ('USER', 'API User', 'Default API user role', 'ENABLED'),
    ('MANAGER', 'Manager', 'Default manager role', 'ENABLED'),
    ('SUPER_ADMIN', 'Super Admin', 'Default super administrator role', 'ENABLED');

INSERT IGNORE INTO sys_permission (permission_code, permission_name, description, status)
VALUES
    ('user:profile:read', 'Read user profile', 'Allows reading the current API user profile', 'ENABLED'),
    ('manager:login', 'Manager login', 'Allows login through the manager login endpoint', 'ENABLED'),
    ('manager:dashboard:read', 'Read manager dashboard', 'Allows reading manager dashboard data', 'ENABLED');

INSERT IGNORE INTO sys_menu (menu_code, menu_name, path, component, icon, sort_order, visible, status)
VALUES
    ('user-profile', 'User Profile', '/api/users/me', 'UserProfile', 'User', 10, 1, 'ENABLED'),
    ('manager-dashboard', 'Manager Dashboard', '/api/manager/dashboard', 'ManagerDashboard', 'LayoutDashboard', 20, 1, 'ENABLED'),
    ('system-management', 'System Management', '/api/manager/system', 'SystemManagement', 'Settings', 30, 1, 'ENABLED');

INSERT IGNORE INTO sys_menu (parent_id, menu_code, menu_name, path, component, icon, sort_order, visible, status)
SELECT parent.id, 'system-users', 'User Management', '/api/manager/system/users', 'SystemUsers', 'Users', 31, 1, 'ENABLED'
FROM sys_menu parent
WHERE parent.menu_code = 'system-management';

INSERT IGNORE INTO sys_menu (parent_id, menu_code, menu_name, path, component, icon, sort_order, visible, status)
SELECT parent.id, 'system-roles', 'Role Management', '/api/manager/system/roles', 'SystemRoles', 'ShieldCheck', 32, 1, 'ENABLED'
FROM sys_menu parent
WHERE parent.menu_code = 'system-management';

INSERT IGNORE INTO sys_menu (parent_id, menu_code, menu_name, path, component, icon, sort_order, visible, status)
SELECT parent.id, 'system-permissions', 'Permission Management', '/api/manager/system/permissions', 'SystemPermissions', 'KeyRound', 33, 1, 'ENABLED'
FROM sys_menu parent
WHERE parent.menu_code = 'system-management';

INSERT IGNORE INTO sys_user_role (user_id, role_id)
SELECT u.id, r.id FROM sys_user u, sys_role r
WHERE u.username = 'alice' AND r.role_code = 'USER';

INSERT IGNORE INTO sys_user_role (user_id, role_id)
SELECT u.id, r.id FROM sys_user u, sys_role r
WHERE u.username = 'manager' AND r.role_code = 'MANAGER';

INSERT IGNORE INTO sys_user_role (user_id, role_id)
SELECT u.id, r.id FROM sys_user u, sys_role r
WHERE u.username = 'admin' AND r.role_code = 'SUPER_ADMIN';

INSERT IGNORE INTO sys_role_permission (role_id, permission_id)
SELECT r.id, p.id FROM sys_role r, sys_permission p
WHERE r.role_code = 'USER' AND p.permission_code = 'user:profile:read';

INSERT IGNORE INTO sys_role_permission (role_id, permission_id)
SELECT r.id, p.id FROM sys_role r, sys_permission p
WHERE r.role_code = 'MANAGER'
  AND p.permission_code IN ('manager:login', 'manager:dashboard:read', 'user:profile:read');

INSERT IGNORE INTO sys_role_permission (role_id, permission_id)
SELECT r.id, p.id FROM sys_role r, sys_permission p
WHERE r.role_code = 'SUPER_ADMIN';

INSERT IGNORE INTO sys_role_menu (role_id, menu_id)
SELECT r.id, m.id FROM sys_role r, sys_menu m
WHERE r.role_code = 'USER' AND m.menu_code = 'user-profile';

INSERT IGNORE INTO sys_role_menu (role_id, menu_id)
SELECT r.id, m.id FROM sys_role r, sys_menu m
WHERE r.role_code = 'MANAGER'
  AND m.menu_code IN ('manager-dashboard', 'system-management');

INSERT IGNORE INTO sys_role_menu (role_id, menu_id)
SELECT r.id, m.id FROM sys_role r, sys_menu m
WHERE r.role_code = 'SUPER_ADMIN'
  AND m.menu_code IN ('manager-dashboard', 'system-management', 'system-users', 'system-roles', 'system-permissions');
