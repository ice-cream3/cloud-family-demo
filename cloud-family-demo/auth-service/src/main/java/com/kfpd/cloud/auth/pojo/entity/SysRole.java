package com.kfpd.cloud.auth.pojo.entity;

import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * 系统角色，对应 fa-cloud.sys_role 表。
 */
@Data
@TableName("sys_role")
public class SysRole {

    /**
     * 主键 ID。
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 角色唯一编码。
     */
    @TableField("role_code")
    private String roleCode;

    /**
     * 角色名称。
     */
    @TableField("role_name")
    private String roleName;

    /**
     * 角色说明。
     */
    @TableField("description")
    private String description;

    /**
     * 角色状态：ENABLED 启用、DISABLED 禁用。
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
