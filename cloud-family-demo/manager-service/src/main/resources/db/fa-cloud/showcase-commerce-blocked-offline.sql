USE `fa-cloud`;

-- 停用商品同步下架，防止旧记录同时呈现已上架与后台停用。
UPDATE showcase_product SET status='OFFLINE' WHERE moderation_status='BLOCKED' AND status='PUBLISHED';
