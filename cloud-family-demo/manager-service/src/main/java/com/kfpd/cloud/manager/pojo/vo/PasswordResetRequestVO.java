package com.kfpd.cloud.manager.pojo.vo;

import jakarta.validation.constraints.Size;

public record PasswordResetRequestVO(
        @Size(max = 255, message = "密码摘要长度不能超过255个字符")
        String passwordHash,
        @Size(max = 255, message = "密码长度不能超过255个字符")
        String password
) {

    public String resolvedPasswordHash() {
        return passwordHash == null || passwordHash.isBlank() ? password : passwordHash;
    }
}
