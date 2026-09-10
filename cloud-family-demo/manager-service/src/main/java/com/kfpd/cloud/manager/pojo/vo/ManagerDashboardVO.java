package com.kfpd.cloud.manager.pojo.vo;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Demo dashboard payload returned by the manager service.
 */
public record ManagerDashboardVO(
        String username,
        String displayName,
        List<String> roles,
        List<String> permissions,
        LocalDateTime requestTime
) {
}
