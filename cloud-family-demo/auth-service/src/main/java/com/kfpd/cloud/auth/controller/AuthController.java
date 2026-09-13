package com.kfpd.cloud.auth.controller;

import com.kfpd.cloud.auth.pojo.vo.LoginVO;
import com.kfpd.cloud.auth.pojo.dto.LoginRequestContext;
import com.kfpd.cloud.auth.pojo.dto.LoginResponse;
import com.kfpd.cloud.auth.pojo.vo.KickOutVO;
import com.kfpd.cloud.auth.pojo.vo.RefreshTokenVO;
import com.kfpd.cloud.auth.pojo.dto.TokenValidation;
import com.kfpd.cloud.auth.service.AuthService;
import com.kfpd.cloud.common.web.ApiResponse;
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
    public ApiResponse<LoginResponse> apiLogin(@Valid @RequestBody LoginVO request, HttpServletRequest servletRequest) {
        // API users are rate-limited by username and client IP in AuthService.
        return ApiResponse.success(authService.apiLogin(request, resolveLoginRequestContext(servletRequest)));
    }

    @PostMapping("/manager/login")
    public ApiResponse<LoginResponse> managerLogin(@Valid @RequestBody LoginVO request, HttpServletRequest servletRequest) {
        // Manager login has stricter checks, including IP allowlist and login permission.
        return ApiResponse.success(authService.managerLogin(request, resolveLoginRequestContext(servletRequest)));
    }

    @PostMapping("/refresh")
    public ApiResponse<LoginResponse> refresh(@Valid @RequestBody RefreshTokenVO request) {
        return ApiResponse.success(authService.refreshAccessToken(request));
    }

    @PostMapping("/logout")
    public ApiResponse<Void> logout(@RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization) {
        authService.logout(authorization);
        return ApiResponse.success();
    }

    @PostMapping("/manager/kick-out")
    public ApiResponse<Integer> kickOut(@Valid @RequestBody KickOutVO request,
                                        @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization) {
        return ApiResponse.success(authService.kickOut(request, authorization));
    }

    @PostMapping("/validate")
    public ApiResponse<TokenValidation> validate(@RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization) {
        return ApiResponse.success(authService.validate(authorization));
    }

    private LoginRequestContext resolveLoginRequestContext(HttpServletRequest request) {
        String userAgent = request.getHeader(HttpHeaders.USER_AGENT);
        return new LoginRequestContext(
                resolveClientIp(request),
                blankToNull(userAgent),
                resolveDeviceType(userAgent),
                resolveBrowser(userAgent),
                resolveOperatingSystem(userAgent)
        );
    }

    private String resolveClientIp(HttpServletRequest request) {
        // When traffic comes through a proxy or gateway, the original client IP is usually forwarded here.
        String forwardedFor = request.getHeader("X-Forwarded-For");
        if (forwardedFor != null && !forwardedFor.isBlank()) {
            return forwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    private String resolveDeviceType(String userAgent) {
        String value = lower(userAgent);
        if (value == null) {
            return "UNKNOWN";
        }
        if (value.contains("ipad") || value.contains("tablet")) {
            return "TABLET";
        }
        if (value.contains("mobile") || value.contains("android") || value.contains("iphone")) {
            return "MOBILE";
        }
        return "DESKTOP";
    }

    private String resolveBrowser(String userAgent) {
        String value = lower(userAgent);
        if (value == null) {
            return "UNKNOWN";
        }
        if (value.contains("edg/")) {
            return "Edge";
        }
        if (value.contains("chrome/") || value.contains("crios/")) {
            return "Chrome";
        }
        if (value.contains("firefox/") || value.contains("fxios/")) {
            return "Firefox";
        }
        if (value.contains("safari/")) {
            return "Safari";
        }
        return "Other";
    }

    private String resolveOperatingSystem(String userAgent) {
        String value = lower(userAgent);
        if (value == null) {
            return "UNKNOWN";
        }
        if (value.contains("windows")) {
            return "Windows";
        }
        if (value.contains("android")) {
            return "Android";
        }
        if (value.contains("iphone") || value.contains("ipad") || value.contains("ios")) {
            return "iOS";
        }
        if (value.contains("mac os") || value.contains("macintosh")) {
            return "macOS";
        }
        if (value.contains("linux")) {
            return "Linux";
        }
        return "Other";
    }

    private String lower(String value) {
        return value == null || value.isBlank() ? null : value.toLowerCase();
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value;
    }
}
