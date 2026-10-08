package com.kfpd.cloud.partner.pojo.vo;

public record PasswordChangeVO(
        String currentPassword,
        String newPassword
) {
}
