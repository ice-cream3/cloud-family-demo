package com.kfpd.cloud.manager.pojo.vo;

public record SysUserRequestVO(
        String username,
        String passwordHash,
        String password,
        String displayName,
        String email,
        String status
) {

    public String resolvedPasswordHash() {
        return passwordHash == null || passwordHash.isBlank() ? password : passwordHash;
    }
}
