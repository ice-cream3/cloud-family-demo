package com.kfpd.cloud.auth.pojo;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Token response returned after a successful login.
 */
public record LoginResponse(
        String tokenType,
        String accessToken,
        String username,
        String userType,
        List<String> roles,
        List<String> permissions,
        LocalDateTime expiresAt
) {
}
