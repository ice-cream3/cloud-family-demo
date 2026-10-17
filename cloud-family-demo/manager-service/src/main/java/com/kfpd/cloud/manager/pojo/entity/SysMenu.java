package com.kfpd.cloud.manager.pojo.entity;

import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * 系统菜单，对应 fa-cloud.sys_menu 表。
 */
@Data
@TableName("sys_menu")
public class SysMenu {

    /**
     * 主键 ID。
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 父级菜单 ID，顶级菜单可为空。
     */
    private Long parentId;

    /**
     * 菜单唯一编码。
     */
    private String menuCode;

    /**
     * 菜单名称。
     */
    private String menuName;

    /**
     * 菜单路由或接口路径。
     */
    private String path;

    /**
     * 前端组件名称。
     */
    private String component;

    /**
     * 菜单图标标识。
     */
    private String icon;

    /**
     * 菜单层级，从 1 开始。
     */
    private Integer menuLevel;

    /**
     * 是否为按钮菜单。
     */
    private Boolean buttonFlag;

    /**
     * 显示排序值。
     */
    private Integer sortOrder;

    /**
     * 是否在菜单中显示。
     */
    private Boolean visible;

    /**
     * 菜单状态：ENABLED 启用、DISABLED 禁用。
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
