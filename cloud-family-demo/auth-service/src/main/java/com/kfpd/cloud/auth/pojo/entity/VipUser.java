package com.kfpd.cloud.auth.pojo.entity;

import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * 会员用户，对应 fa-cloud.vip_user 表。
 */
@Data
@TableName("vip_user")
public class VipUser {

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
     * 电话号码国际区号，例如 +86。
     */
    @TableField("phone_country_code")
    private String phoneCountryCode;

    /**
     * 电话号码（不包含国际区号）。
     */
    @TableField("phone")
    private String phone;

    /**
     * 会员等级。
     */
    @TableField("vip_level")
    private String vipLevel;

    /**
     * 行程预约信用积分。
     */
    @TableField("credit_score")
    private Integer creditScore;

    /**
     * 会员账号状态：ENABLED 启用、DISABLED 禁用。
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
