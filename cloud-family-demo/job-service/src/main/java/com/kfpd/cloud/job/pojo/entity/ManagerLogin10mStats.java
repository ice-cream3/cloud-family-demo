package com.kfpd.cloud.job.pojo.entity;

import java.time.Instant;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * 管理端用户十分钟登录统计，对应 fa-model.manager_login_10m_stats 表。
 */
@Data
@TableName("manager_login_10m_stats")
public class ManagerLogin10mStats {

    /**
     * 主键 ID。
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 统计窗口开始时间（包含边界）。
     */
    @TableField("window_start_at")
    private Instant windowStartAt;

    /**
     * 统计窗口结束时间（不包含边界）。
     */
    @TableField("window_end_at")
    private Instant windowEndAt;

    /**
     * 用户账号。
     */
    @TableField("username")
    private String username;

    /**
     * 窗口内成功登录次数。
     */
    @TableField("login_count")
    private Long loginCount;

    /**
     * 窗口内首次成功登录时间。
     */
    @TableField("first_login_at")
    private Instant firstLoginAt;

    /**
     * 窗口内最后一次成功登录时间。
     */
    @TableField("last_login_at")
    private Instant lastLoginAt;

    /**
     * 统计计算时间。
     */
    @TableField("calculated_at")
    private Instant calculatedAt;

    /**
     * 更新时间。
     */
    @TableField("updated_at")
    private Instant updatedAt;
}
