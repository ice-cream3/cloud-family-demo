package com.kfpd.cloud.manager.pojo.entity;

import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * 角色权限关联，对应 fa-cloud.sys_role_permission 表。
 * 联合主键：role_id、permission_id；读写时需同时指定全部主键字段。
 */
@Data
@TableName("sys_role_permission")
public class SysRolePermission {

    /**
     * 角色 ID（sys_role.id）。
     */
    @TableField("role_id")
    private Long roleId;

    /**
     * 权限 ID（sys_permission.id）。
     */
    @TableField("permission_id")
    private Long permissionId;

    /**
     * 创建时间。
     */
    @TableField("created_at")
    private LocalDateTime createdAt;
}
