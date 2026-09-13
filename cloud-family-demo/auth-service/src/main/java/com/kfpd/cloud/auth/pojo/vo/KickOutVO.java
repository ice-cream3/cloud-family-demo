package com.kfpd.cloud.auth.pojo.vo;

import jakarta.validation.constraints.NotBlank;

/**
 * Manager request to revoke all active sessions for one account.
 */
public record KickOutVO(@NotBlank String username) {
}
