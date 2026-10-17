package com.kfpd.cloud.partner.pojo.entity;

import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * 用户历史记录，对应 fa-cloud.user_history 表。
 */
@Data
@TableName("user_history")
public class UserHistory {

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
     * 历史分类。
     */
    @TableField("category")
    private String category;

    /**
     * 历史标题。
     */
    @TableField("title")
    private String title;

    /**
     * 历史描述。
     */
    @TableField("description")
    private String description;

    /**
     * 图标色调。
     */
    @TableField("icon_tone")
    private String iconTone;

    /**
     * 发生时间。
     */
    @TableField("occurred_at")
    private LocalDateTime occurredAt;
}
