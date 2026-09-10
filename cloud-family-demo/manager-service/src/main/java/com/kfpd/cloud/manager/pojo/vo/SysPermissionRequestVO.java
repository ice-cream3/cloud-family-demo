package com.kfpd.cloud.manager.pojo.vo;

public record SysPermissionRequestVO(
        String permissionCode,
        String permissionName,
        String description,
        String status
) {
}
