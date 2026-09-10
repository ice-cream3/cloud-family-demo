package com.kfpd.cloud.manager.pojo.vo;

public record SysRoleRequestVO(
        String roleCode,
        String roleName,
        String description,
        String status
) {
}
