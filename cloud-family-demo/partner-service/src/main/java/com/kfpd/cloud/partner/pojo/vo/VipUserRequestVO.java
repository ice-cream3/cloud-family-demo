package com.kfpd.cloud.partner.pojo.vo;

public record VipUserRequestVO(
        String username,
        String passwordHash,
        String displayName,
        String email,
        String phone,
        String vipLevel,
        String status
) {
}
