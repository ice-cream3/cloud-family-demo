package com.kfpd.cloud.partner.pojo;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Demo user profile returned by the partner service.
 */
public record UserProfile(String username, String displayName, List<String> roles, LocalDateTime requestTime) {
}
