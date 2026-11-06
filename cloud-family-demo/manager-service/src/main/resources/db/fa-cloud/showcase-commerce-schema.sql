USE `fa-cloud`;

-- 一用户一店铺。店铺状态由后台控制，用户修改资料不改变后台停用状态。
CREATE TABLE IF NOT EXISTS showcase_shop (
    id bigint NOT NULL AUTO_INCREMENT COMMENT '商家ID',
    owner_username varchar(64) NOT NULL COMMENT '所属用户账号',
    name varchar(100) NOT NULL COMMENT '店铺名称',
    cover_url varchar(1024) DEFAULT NULL COMMENT '店铺封面',
    description varchar(2000) NOT NULL COMMENT '店铺介绍',
    contact varchar(100) DEFAULT NULL COMMENT '商家自填公开联系方式',
    status varchar(16) NOT NULL DEFAULT 'PENDING' COMMENT 'PENDING待审核/ACTIVE通过/REJECTED驳回/BLOCKED停用',
    sort_order int NOT NULL DEFAULT 0 COMMENT '后台排序',
    created_at timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_showcase_shop_owner (owner_username),
    KEY idx_showcase_shop_status_sort (status, sort_order, id),
    CONSTRAINT chk_showcase_shop_status CHECK (status IN ('PENDING','ACTIVE','REJECTED','BLOCKED'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='用户店铺';

CREATE TABLE IF NOT EXISTS showcase_product (
    id bigint NOT NULL AUTO_INCREMENT COMMENT '商品ID',
    shop_id bigint NOT NULL COMMENT '所属商家，不允许通过编辑转移归属',
    title varchar(100) NOT NULL COMMENT '商品名称',
    category varchar(32) NOT NULL COMMENT '商品分类',
    cover_url varchar(1024) DEFAULT NULL,
    summary varchar(300) NOT NULL COMMENT '商品简介',
    content mediumtext NOT NULL COMMENT '商品详情，纯文本',
    price decimal(12,2) NOT NULL COMMENT '展示价格，人民币元',
    stock int NOT NULL DEFAULT 0 COMMENT '展示库存',
    status varchar(16) NOT NULL DEFAULT 'DRAFT' COMMENT '用户状态：DRAFT/PUBLISHED/OFFLINE',
    moderation_status varchar(16) NOT NULL DEFAULT 'ACTIVE' COMMENT '后台状态：ACTIVE/BLOCKED',
    moderation_reason varchar(500) DEFAULT NULL COMMENT '系统停用原因，商家只读',
    sort_order int NOT NULL DEFAULT 0,
    published_at timestamp NULL DEFAULT NULL,
    created_at timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_showcase_product_shop_state_sort (shop_id, status, moderation_status, sort_order, id),

    CONSTRAINT chk_showcase_product_status CHECK (status IN ('DRAFT','PUBLISHED','OFFLINE')),
    CONSTRAINT chk_showcase_product_moderation CHECK (moderation_status IN ('PENDING','ACTIVE','REJECTED','BLOCKED')),
    CONSTRAINT chk_showcase_product_values CHECK (price >= 0 AND stock >= 0 AND sort_order >= 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='用户商品';
-- 原 showcase_item 内容表保留，不推断商家归属，不自动转换为商品。
