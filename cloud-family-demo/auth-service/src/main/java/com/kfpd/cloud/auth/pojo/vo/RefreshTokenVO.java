package com.kfpd.cloud.auth.pojo.vo;

import jakarta.validation.constraints.NotBlank;

/**
 * Refresh token payload used to issue a new access token.
 */
public record RefreshTokenVO(@NotBlank String refreshToken) {
}
