package com.kfpd.cloud.manager.pojo.dto;

public record SysPermissionRequestDTO(
        String permissionCode,
        String permissionName,
        String description,
        String status
) {
}
