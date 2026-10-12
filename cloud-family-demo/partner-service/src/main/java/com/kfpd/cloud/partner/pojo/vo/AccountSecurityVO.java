package com.kfpd.cloud.partner.pojo.vo;

import java.time.LocalDateTime;

public record AccountSecurityVO(
        String username,
        String displayName,
        String email,
        String phoneCountryCode,
        String phone,
        String vipLevel,
        Integer creditScore,
        String status,
        LocalDateTime updatedAt
) {
}
