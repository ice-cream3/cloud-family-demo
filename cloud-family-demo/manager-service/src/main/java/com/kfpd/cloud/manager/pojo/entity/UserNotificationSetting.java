package com.kfpd.cloud.manager.pojo.entity;

import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * 用户通知设置，对应 fa-cloud.user_notification_setting 表。
 */
@Data
@TableName("user_notification_setting")
public class UserNotificationSetting {

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
     * 系统通知开关。
     */
    @TableField("system_enabled")
    private Boolean systemEnabled;

    /**
     * 活动通知开关。
     */
    @TableField("activity_enabled")
    private Boolean activityEnabled;

    /**
     * 任务通知开关。
     */
    @TableField("task_enabled")
    private Boolean taskEnabled;

    /**
     * 更新时间。
     */
    @TableField("updated_at")
    private LocalDateTime updatedAt;
}
