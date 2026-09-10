package com.kfpd.cloud.manager.pojo.entity;

import java.time.LocalDateTime;

import lombok.Data;

@Data
public class SysPermission {

    private Long id;
    private String permissionCode;
    private String permissionName;
    private String description;
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
