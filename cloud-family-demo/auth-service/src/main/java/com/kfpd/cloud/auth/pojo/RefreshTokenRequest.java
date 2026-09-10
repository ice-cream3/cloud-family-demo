package com.kfpd.cloud.auth.pojo;

import jakarta.validation.constraints.NotBlank;

/**
 * Refresh token payload used to issue a new access token.
 */
public record RefreshTokenRequest(@NotBlank String refreshToken) {
}
