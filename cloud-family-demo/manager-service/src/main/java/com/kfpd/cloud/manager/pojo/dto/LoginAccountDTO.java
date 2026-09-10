package com.kfpd.cloud.manager.pojo.dto;

import java.util.List;

public record LoginAccountDTO(
        String username,
        String passwordHash,
        String userType,
        List<String> roles,
        List<String> permissions
) {
}
