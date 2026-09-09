package com.kfpd.cloud.auth.pojo;

import java.time.LocalDateTime;
import java.util.List;

/**
 * In-memory session data associated with an issued access token.
 */
public record SessionInfo(
        String username,
        String userType,
        List<String> roles,
        List<String> permissions,
        LocalDateTime expiresAt
) {
}
