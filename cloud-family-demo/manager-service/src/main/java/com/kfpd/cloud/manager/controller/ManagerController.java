package com.kfpd.cloud.manager.controller;

import java.util.Map;

import com.kfpd.cloud.common.web.ApiResponse;
import com.kfpd.cloud.common.web.GatewayHeaders;
import com.kfpd.cloud.manager.pojo.vo.ManagerDashboardVO;
import com.kfpd.cloud.manager.service.ManagerService;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/manager")
public class ManagerController {

    private final ManagerService managerService;

    public ManagerController(ManagerService managerService) {
        this.managerService = managerService;
    }

    @GetMapping("/dashboard")
    public ApiResponse<ManagerDashboardVO> dashboard(@RequestHeader(value = GatewayHeaders.USER_NAME, defaultValue = "anonymous") String username,
                                      @RequestHeader(value = GatewayHeaders.USER_ROLES, defaultValue = "") String roles,
                                      @RequestHeader(value = GatewayHeaders.USER_PERMISSIONS, defaultValue = "") String permissions) {
        // These headers are injected by gateway after token validation.
        return ApiResponse.success(managerService.dashboard(username, roles, permissions));
    }

    @GetMapping("/health")
    public ApiResponse<Map<String, String>> health() {
        return ApiResponse.success(Map.of("status", "UP", "service", "manager-service"));
    }
}
