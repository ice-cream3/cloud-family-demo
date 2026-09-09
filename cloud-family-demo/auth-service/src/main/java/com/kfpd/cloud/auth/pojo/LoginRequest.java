package com.kfpd.cloud.auth.pojo;

import jakarta.validation.constraints.NotBlank;

/**
 * Login credentials shared by API and manager login endpoints.
 */
public record LoginRequest(@NotBlank String username, @NotBlank String password) {
}
