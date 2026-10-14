package com.kfpd.cloud.manager.pojo.vo;

import java.time.Instant;

public record AuthSessionStatusVO(
        String username,
        boolean online,
        int sessionCount,
        Instant latestAccessTokenExpiresAt,
        Instant latestRefreshTokenExpiresAt
) {
}
