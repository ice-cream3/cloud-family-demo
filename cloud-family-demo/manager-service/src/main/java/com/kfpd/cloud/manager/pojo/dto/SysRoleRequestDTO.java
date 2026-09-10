package com.kfpd.cloud.manager.pojo.dto;

public record SysRoleRequestDTO(
        String roleCode,
        String roleName,
        String description,
        String status
) {
}
