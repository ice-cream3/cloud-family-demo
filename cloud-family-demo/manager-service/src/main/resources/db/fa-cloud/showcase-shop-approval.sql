USE `fa-cloud`;
-- 已有店铺保留原状态；新申请默认待审核。先执行本迁移再部署服务。
ALTER TABLE showcase_shop
    DROP CHECK chk_showcase_shop_status,
    MODIFY status varchar(16) NOT NULL DEFAULT 'PENDING' COMMENT 'PENDING待审核/ACTIVE通过/REJECTED驳回/BLOCKED停用',
    ADD CONSTRAINT chk_showcase_shop_status CHECK (status IN ('PENDING','ACTIVE','REJECTED','BLOCKED'));
