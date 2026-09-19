package com.kfpd.cloud.manager.pojo.vo;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SysUserRequestVO(
        @NotBlank(message = "账号不能为空")
        @Size(max = 64, message = "账号长度不能超过64个字符")
        String username,
        @Size(max = 255, message = "密码摘要长度不能超过255个字符")
        String passwordHash,
        @Size(max = 255, message = "密码长度不能超过255个字符")
        String password,
        @NotBlank(message = "显示名称不能为空")
        @Size(max = 100, message = "显示名称长度不能超过100个字符")
        String displayName,
        @Email(message = "邮箱格式不正确")
        @Size(max = 255, message = "邮箱长度不能超过255个字符")
        String email,
        @Size(max = 32, message = "状态长度不能超过32个字符")
        String status
) {

    public String resolvedPasswordHash() {
        return passwordHash == null || passwordHash.isBlank() ? password : passwordHash;
    }
}
