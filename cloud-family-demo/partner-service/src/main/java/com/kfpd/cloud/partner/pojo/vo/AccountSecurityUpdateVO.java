package com.kfpd.cloud.partner.pojo.vo;

public record AccountSecurityUpdateVO(
        String displayName,
        String email,
        String phoneCountryCode,
        String phone,
        String verificationCode
) {
}
