package com.kfpd.cloud.auth.pojo;

import java.time.Instant;
import java.util.List;

import org.springframework.security.oauth2.core.AuthorizationGrantType;

public record RedisLoginSession(
        String accessToken,
        String refreshToken,
        String username,
        String userType,
        List<String> roles,
        List<String> permissions,
        String clientId,
        String grantType,
        Instant accessTokenIssuedAt,
        Instant accessTokenExpiresAt,
        Instant refreshTokenIssuedAt,
        Instant refreshTokenExpiresAt
) {

    public AuthorizationGrantType authorizationGrantType() {
        return new AuthorizationGrantType(grantType);
    }
}
