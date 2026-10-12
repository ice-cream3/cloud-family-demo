package com.kfpd.cloud.manager.pojo.vo;

import java.time.LocalDateTime;

public record VipUserVO(
        Long id,
        String username,
        String displayName,
        String email,
        String phoneCountryCode,
        String phone,
        String vipLevel,
        Integer creditScore,
        String status,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        Boolean online,
        Integer activeSessionCount,
        String latestAccessTokenExpiresAt,
        String latestRefreshTokenExpiresAt
) {
}
