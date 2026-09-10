package com.kfpd.cloud.auth.controller;

import com.kfpd.cloud.auth.pojo.LoginRequest;
import com.kfpd.cloud.auth.pojo.LoginResponse;
import com.kfpd.cloud.auth.pojo.RefreshTokenRequest;
import com.kfpd.cloud.auth.pojo.TokenValidation;
import com.kfpd.cloud.auth.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;

import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/api/login")
    public LoginResponse apiLogin(@Valid @RequestBody LoginRequest request, HttpServletRequest servletRequest) {
        // API users are rate-limited by username and client IP in AuthService.
        return authService.apiLogin(request, resolveClientIp(servletRequest));
    }

    @PostMapping("/manager/login")
    public LoginResponse managerLogin(@Valid @RequestBody LoginRequest request, HttpServletRequest servletRequest) {
        // Manager login has stricter checks, including IP allowlist and login permission.
        return authService.managerLogin(request, resolveClientIp(servletRequest));
    }

    @PostMapping("/refresh")
    public LoginResponse refresh(@Valid @RequestBody RefreshTokenRequest request) {
        return authService.refreshAccessToken(request);
    }

    @PostMapping("/validate")
    public TokenValidation validate(@RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization) {
        return authService.validate(authorization);
    }

    private String resolveClientIp(HttpServletRequest request) {
        // When traffic comes through a proxy or gateway, the original client IP is usually forwarded here.
        String forwardedFor = request.getHeader("X-Forwarded-For");
        if (forwardedFor != null && !forwardedFor.isBlank()) {
            return forwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
