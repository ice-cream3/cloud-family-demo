package com.kfpd.cloud.manager.pojo.entity;

import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * 菜单权限关联，对应 fa-cloud.sys_menu_permission 表。
 * 联合主键：menu_id、permission_id；读写时需同时指定全部主键字段。
 */
@Data
@TableName("sys_menu_permission")
public class SysMenuPermission {

    /**
     * 菜单 ID（sys_menu.id）。
     */
    @TableField("menu_id")
    private Long menuId;

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
