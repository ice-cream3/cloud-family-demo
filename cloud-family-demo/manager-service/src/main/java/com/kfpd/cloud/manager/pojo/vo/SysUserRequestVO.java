package com.kfpd.cloud.manager.pojo.vo;

public record SysUserRequestVO(
        String username,
        String passwordHash,
        String displayName,
        String email,
        String status
) {
}
