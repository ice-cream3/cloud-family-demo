USE `fa-cloud`;
-- 已有字典表补齐字段注释；字段类型、默认值和约束保持一致，可重复执行。
ALTER TABLE sys_dict_type
 MODIFY COLUMN id bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID，自增',
 MODIFY COLUMN type_code varchar(64) NOT NULL COMMENT '字典类型编码，全局唯一，创建后不可修改',
 MODIFY COLUMN name varchar(100) NOT NULL COMMENT '字典类型名称',
 MODIFY COLUMN description varchar(500) DEFAULT NULL COMMENT '字典类型说明',
 MODIFY COLUMN status varchar(16) NOT NULL DEFAULT 'ENABLED' COMMENT '状态：ENABLED启用，DISABLED停用',
 MODIFY COLUMN sort_order int NOT NULL DEFAULT 0 COMMENT '排序值，非负整数，越小越靠前',
 MODIFY COLUMN created_at timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
 MODIFY COLUMN updated_at timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '最后更新时间';

ALTER TABLE sys_dict_item
 MODIFY COLUMN id bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID，自增',
 MODIFY COLUMN type_code varchar(64) NOT NULL COMMENT '所属字典类型编码，关联sys_dict_type.type_code，创建后不可修改',
 MODIFY COLUMN item_code varchar(64) NOT NULL COMMENT '字典项编码，同一类型内唯一，创建后不可修改',
 MODIFY COLUMN name varchar(100) NOT NULL COMMENT '字典项名称，用于页面展示',
 MODIFY COLUMN description varchar(500) DEFAULT NULL COMMENT '字典项说明',
 MODIFY COLUMN status varchar(16) NOT NULL DEFAULT 'ENABLED' COMMENT '状态：ENABLED启用，DISABLED停用',
 MODIFY COLUMN sort_order int NOT NULL DEFAULT 0 COMMENT '排序值，非负整数，越小越靠前',
 MODIFY COLUMN created_at timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
 MODIFY COLUMN updated_at timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '最后更新时间';

