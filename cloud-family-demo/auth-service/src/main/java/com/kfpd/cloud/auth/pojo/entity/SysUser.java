package com.kfpd.cloud.auth.pojo.entity;

import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * 系统用户，对应 fa-cloud.sys_user 表。
 */
@Data
@TableName("sys_user")
public class SysUser {

    /**
     * 主键 ID。
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 用户账号。
     */
    @TableField("username")
    private String username;

    /**
     * 登录密码哈希值。
     */
    @TableField("password_hash")
    private String passwordHash;

    /**
     * 用户显示名称。
     */
    @TableField("display_name")
    private String displayName;

    /**
     * 电子邮箱地址。
     */
    @TableField("email")
    private String email;

    /**
     * 用户状态：ENABLED 启用、DISABLED 禁用。
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
