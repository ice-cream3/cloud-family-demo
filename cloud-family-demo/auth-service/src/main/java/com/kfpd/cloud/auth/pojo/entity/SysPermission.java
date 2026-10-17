package com.kfpd.cloud.auth.pojo.entity;

import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * 系统权限，对应 fa-cloud.sys_permission 表。
 */
@Data
@TableName("sys_permission")
public class SysPermission {

    /**
     * 主键 ID。
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 权限唯一编码。
     */
    @TableField("permission_code")
    private String permissionCode;

    /**
     * 权限名称。
     */
    @TableField("permission_name")
    private String permissionName;

    /**
     * 权限说明。
     */
    @TableField("description")
    private String description;

    /**
     * 权限状态：ENABLED 启用、DISABLED 禁用。
     */
    @TableField("status")
    private String status;

    /**
     * 创建时间。
     */
    @TableField("created_at")
    private LocalDateTime createdAt;

    /**
     * 更新时间。
     */
    @TableField("updated_at")
    private LocalDateTime updatedAt;
}
