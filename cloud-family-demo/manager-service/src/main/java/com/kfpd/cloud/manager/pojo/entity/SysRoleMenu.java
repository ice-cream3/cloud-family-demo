package com.kfpd.cloud.manager.pojo.entity;

import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * 角色菜单关联，对应 fa-cloud.sys_role_menu 表。
 * 联合主键：role_id、menu_id；读写时需同时指定全部主键字段。
 */
@Data
@TableName("sys_role_menu")
public class SysRoleMenu {

    /**
     * 角色 ID（sys_role.id）。
     */
    @TableField("role_id")
    private Long roleId;

    /**
     * 菜单 ID（sys_menu.id）。
     */
    @TableField("menu_id")
    private Long menuId;

    /**
     * 创建时间。
     */
    @TableField("created_at")
    private LocalDateTime createdAt;
}
