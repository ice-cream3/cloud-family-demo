package com.kfpd.cloud.auth.pojo.vo;

import jakarta.validation.constraints.NotBlank;

/**
 * Login credentials shared by API and manager login endpoints.
 */
public record LoginVO(@NotBlank String username, @NotBlank String password) {
}
