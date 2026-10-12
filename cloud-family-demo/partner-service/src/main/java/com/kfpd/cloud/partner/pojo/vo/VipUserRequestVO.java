package com.kfpd.cloud.partner.pojo.vo;

public record VipUserRequestVO(
        String username,
        String passwordHash,
        String displayName,
        String email,
        String phoneCountryCode,
        String phone,
        String vipLevel,
        Integer creditScore,
        String status
) {
}
