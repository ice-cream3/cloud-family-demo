package com.kfpd.cloud.partner.pojo.vo;

public record PhoneVerificationCodeVO(
        String phoneCountryCode,
        String phone,
        Integer expiresInSeconds
) {
}
