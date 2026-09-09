package com.kfpd.cloud.manager.service;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import com.kfpd.cloud.manager.pojo.ManagerDashboard;

import org.springframework.stereotype.Service;

@Service
public class ManagerService {

    public ManagerDashboard dashboard(String username, String roles, String permissions) {
        return new ManagerDashboard(
                username,
                "manager-user",
                splitHeader(roles),
                splitHeader(permissions),
                LocalDateTime.now()
        );
    }

    private List<String> splitHeader(String value) {
        // Gateway serializes roles and permissions as comma-separated headers for this demo.
        if (value == null || value.isBlank()) {
            return List.of();
        }
        return Arrays.stream(value.split(","))
                .map(String::trim)
                .filter(item -> !item.isEmpty())
                .toList();
    }
}
