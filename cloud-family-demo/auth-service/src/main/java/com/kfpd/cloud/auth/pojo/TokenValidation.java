package com.kfpd.cloud.auth.pojo;

import java.util.List;
import java.util.Map;

/**
 * Validation result consumed by gateway before routing protected requests.
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
