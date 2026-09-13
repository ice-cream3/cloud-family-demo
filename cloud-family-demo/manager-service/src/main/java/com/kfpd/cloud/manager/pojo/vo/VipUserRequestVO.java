package com.kfpd.cloud.manager.pojo.vo;

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
