package com.kfpd.cloud.auth.pojo.vo;

import jakarta.validation.constraints.NotBlank;

/**
 * Public API user registration request.
 */
public record RegisterVO(
        @NotBlank String username,
        @NotBlank String password,
        @NotBlank String displayName,
        String email,
        String phone
) {
}
