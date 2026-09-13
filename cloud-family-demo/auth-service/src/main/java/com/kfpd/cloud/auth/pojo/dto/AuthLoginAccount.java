package com.kfpd.cloud.auth.pojo.dto;

import java.util.List;

public record AuthLoginAccount(
        String username,
        String passwordHash,
        String userType,
        List<String> roles,
        List<String> permissions
) {
}
