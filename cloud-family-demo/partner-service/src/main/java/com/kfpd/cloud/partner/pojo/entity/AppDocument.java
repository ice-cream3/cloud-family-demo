package com.kfpd.cloud.partner.pojo.entity;

import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * 应用协议文档，对应 fa-cloud.app_document 表。
 */
@Data
@TableName("app_document")
public class AppDocument {

    /**
     * 主键ID。
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 文档类型。
     */
    @TableField("document_type")
    private String documentType;

    /**
     * 文档标题。
     */
    @TableField("title")
    private String title;

    /**
     * 文档内容。
     */
    @TableField("content")
    private String content;

    /**
     * 更新时间。
     */
    @TableField("updated_at")
    private LocalDateTime updatedAt;
}
