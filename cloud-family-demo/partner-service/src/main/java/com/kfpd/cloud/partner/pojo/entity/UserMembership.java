package com.kfpd.cloud.partner.pojo.entity;

import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * 用户会员信息，对应 fa-cloud.user_membership 表。
 */
@Data
@TableName("user_membership")
public class UserMembership {

    /**
     * 主键ID。
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 用户账号。
     */
    @TableField("username")
    private String username;

    /**
     * 会员方案名称。
     */
    @TableField("plan_name")
    private String planName;

    /**
     * 会员方案状态，例如 ACTIVE。
     */
    @TableField("status")
    private String status;

    /**
     * 到期时间。
     */
    @TableField("expire_at")
    private LocalDateTime expireAt;

    /**
     * 会员权益说明。
     */
    @TableField("benefits")
    private String benefits;

    /**
     * 更新时间。
     */
    @TableField("updated_at")
    private LocalDateTime updatedAt;
}
