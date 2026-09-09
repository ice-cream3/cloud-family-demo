package com.kfpd.cloud.gateway.pojo;

import java.util.List;
import java.util.Map;

/**
 * Gateway-side view of auth-service token validation response.
 */
public record TokenValidation(
        boolean valid,
        String username,
        String userType,
        List<String> roles,
        List<String> permissions,
        Map<String, Object> claims
) {
}
