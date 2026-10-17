package com.kfpd.cloud.auth.pojo.entity;

import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * 用户角色关联，对应 fa-cloud.sys_user_role 表。
 * 联合主键：user_id、role_id；读写时需同时指定全部主键字段。
 */
@Data
@TableName("sys_user_role")
public class SysUserRole {

    /**
     * 系统用户 ID（sys_user.id）。
     */
    @TableField("user_id")
    private Long userId;

    /**
     * 角色 ID（sys_role.id）。
     */
    @TableField("role_id")
    private Long roleId;

    /**
     * 创建时间。
     */
    @TableField("created_at")
    private LocalDateTime createdAt;
}
