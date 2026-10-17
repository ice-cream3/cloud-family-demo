package com.kfpd.cloud.partner.pojo.entity;

import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.IdType;
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
     * 电话号码国际区号，例如 +86。
     */
    private String phoneCountryCode;

    /**
     * 电话号码（不包含国际区号）。
     */
    private String phone;

    /**
     * 会员等级。
     */
    private String vipLevel;

    /**
     * 行程预约信用积分。
     */
    private Integer creditScore;

    /**
     * 会员账号状态：ENABLED 启用、DISABLED 禁用。
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
