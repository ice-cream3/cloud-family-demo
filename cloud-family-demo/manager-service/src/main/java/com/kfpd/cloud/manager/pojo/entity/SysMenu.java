package com.kfpd.cloud.manager.pojo.entity;

import java.time.LocalDateTime;

import lombok.Data;

@Data
public class SysMenu {

    private Long id;
    private Long parentId;
    private String menuCode;
    private String menuName;
    private String path;
    private String component;
    private String icon;
    private Integer sortOrder;
    private Boolean visible;
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
