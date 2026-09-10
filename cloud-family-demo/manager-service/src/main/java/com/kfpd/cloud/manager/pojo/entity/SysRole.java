package com.kfpd.cloud.manager.pojo.entity;

import java.time.LocalDateTime;

import lombok.Data;

@Data
public class SysRole {

    private Long id;
    private String roleCode;
    private String roleName;
    private String description;
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
