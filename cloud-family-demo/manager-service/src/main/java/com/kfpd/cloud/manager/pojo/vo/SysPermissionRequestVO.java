package com.kfpd.cloud.manager.pojo.vo;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SysPermissionRequestVO(
        @NotBlank(message = "权限编码不能为空")
        @Size(max = 128, message = "权限编码长度不能超过128个字符")
        String permissionCode,
        @NotBlank(message = "权限名称不能为空")
        @Size(max = 100, message = "权限名称长度不能超过100个字符")
        String permissionName,
        @Size(max = 500, message = "描述长度不能超过500个字符")
        String description,
        @Size(max = 32, message = "状态长度不能超过32个字符")
        String status
) {
}
