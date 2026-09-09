package com.kfpd.cloud.userapi.pojo;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Demo user profile returned by the user API service.
 */
public record UserProfile(String username, String displayName, List<String> roles, LocalDateTime requestTime) {
}
