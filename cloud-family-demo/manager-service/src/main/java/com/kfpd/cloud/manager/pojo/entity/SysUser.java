package com.kfpd.cloud.manager.pojo.entity;

import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.IdType;
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
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 用户账号。
     */
    private String username;

    /**
     * 登录密码哈希值。
     */
    private String passwordHash;

    /**
     * 用户显示名称。
     */
    private String displayName;

    /**
     * 电子邮箱地址。
     */
    private String email;

    /**
     * 用户状态：ENABLED 启用、DISABLED 禁用。
     */
    private String status;

    /**
     * 创建时间。
     */
    private LocalDateTime createdAt;

    /**
     * 更新时间。
     */
    private LocalDateTime updatedAt;
}
