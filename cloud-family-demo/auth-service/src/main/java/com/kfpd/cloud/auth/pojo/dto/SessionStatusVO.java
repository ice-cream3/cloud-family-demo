package com.kfpd.cloud.auth.pojo.dto;

import java.time.Instant;

public record SessionStatusVO(
        String username,
        boolean online,
        int sessionCount,
        Instant latestAccessTokenExpiresAt,
        Instant latestRefreshTokenExpiresAt
) {
}
