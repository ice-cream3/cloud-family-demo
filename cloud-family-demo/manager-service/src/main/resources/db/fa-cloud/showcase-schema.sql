USE `fa-cloud`;

-- 精品内容：分类使用名称维护，前台分类从已上架内容中自动聚合。
CREATE TABLE IF NOT EXISTS showcase_item (
    id bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    title varchar(100) NOT NULL COMMENT '标题',
    category varchar(32) NOT NULL COMMENT '分类名称',
    cover_url varchar(1024) DEFAULT NULL COMMENT '封面图片地址',
    summary varchar(300) NOT NULL COMMENT '列表摘要',
    content mediumtext NOT NULL COMMENT '详情正文（纯文本，保留换行）',
    status varchar(16) NOT NULL DEFAULT 'DRAFT' COMMENT 'DRAFT草稿/PUBLISHED上架/OFFLINE下架',
    sort_order int NOT NULL DEFAULT 0 COMMENT '排序，越小越靠前',
    published_at timestamp NULL DEFAULT NULL COMMENT '最近上架时间',
    created_at timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    KEY idx_showcase_status_sort (status, sort_order, id),
    KEY idx_showcase_category_status (category, status),
    CONSTRAINT chk_showcase_status CHECK (status IN ('DRAFT', 'PUBLISHED', 'OFFLINE'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='精品展示内容';
