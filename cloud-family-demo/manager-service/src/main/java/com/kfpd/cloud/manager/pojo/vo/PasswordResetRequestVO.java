package com.kfpd.cloud.manager.pojo.vo;

public record PasswordResetRequestVO(
        String passwordHash,
        String password
) {

    public String resolvedPasswordHash() {
        return passwordHash == null || passwordHash.isBlank() ? password : passwordHash;
    }
}
