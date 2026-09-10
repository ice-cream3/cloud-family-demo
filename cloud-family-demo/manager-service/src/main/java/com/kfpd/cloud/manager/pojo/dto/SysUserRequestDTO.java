package com.kfpd.cloud.manager.pojo.dto;

public record SysUserRequestDTO(
        String username,
        String passwordHash,
        String displayName,
        String email,
        String status
) {
}
