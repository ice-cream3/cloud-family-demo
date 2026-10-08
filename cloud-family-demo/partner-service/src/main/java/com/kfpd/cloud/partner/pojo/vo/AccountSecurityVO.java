package com.kfpd.cloud.partner.pojo.vo;

import java.time.LocalDateTime;

public record AccountSecurityVO(
        String username,
        String displayName,
        String email,
        String phone,
        String vipLevel,
        String status,
        LocalDateTime updatedAt
) {
}
