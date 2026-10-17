package com.kfpd.cloud.partner.pojo.entity;

import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * 应用版本信息，对应 fa-cloud.app_version 表。
 */
@Data
@TableName("app_version")
public class AppVersion {

    /**
     * 主键ID。
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 版本号。
     */
    @TableField("version_name")
    private String versionName;

    /**
     * 是否最新版本。
     */
    @TableField("latest")
    private Boolean latest;

    /**
     * 更新说明。
     */
    @TableField("release_note")
    private String releaseNote;

    /**
     * 创建时间。
     */
    @TableField("created_at")
    private LocalDateTime createdAt;
}
