package com.kfpd.cloud.auth.pojo.dto;

public record LoginRequestContext(
        String clientIp,
        String userAgent,
        String deviceType,
        String browser,
        String operatingSystem
) {
}
