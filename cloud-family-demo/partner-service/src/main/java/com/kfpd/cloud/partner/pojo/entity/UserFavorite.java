package com.kfpd.cloud.partner.pojo.entity;

import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * 用户收藏，对应 fa-cloud.user_favorite 表。
 */
@Data
@TableName("user_favorite")
public class UserFavorite {

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
     * 收藏对象类型。
     */
    @TableField("item_type")
    private String itemType;

    /**
     * 收藏标题。
     */
    @TableField("title")
    private String title;

    /**
     * 收藏描述。
     */
    @TableField("description")
    private String description;

    /**
     * 图标色调。
     */
    @TableField("icon_tone")
    private String iconTone;

    /**
     * 创建时间。
     */
    @TableField("created_at")
    private LocalDateTime createdAt;
}
