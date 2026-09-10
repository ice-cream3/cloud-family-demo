package com.kfpd.cloud.partner.controller;

import java.time.LocalDateTime;
import java.util.Date;
import java.util.Map;

import com.kfpd.cloud.common.web.GatewayHeaders;
import com.kfpd.cloud.partner.pojo.UserProfile;
import com.kfpd.cloud.partner.service.UserService;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping({"/me", "/my"})
    public UserProfile currentUser(
            @RequestHeader(value = GatewayHeaders.USER_NAME, defaultValue = "anonymous") String username,
            @RequestHeader(value = GatewayHeaders.USER_ROLES, required = false) String roles
    ) {
        return userService.currentUser(username, roles);
    }

    public static void main(String[] args) {
        System.out.println(new Date());
        System.out.println(LocalDateTime.now());
    }

    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> health() {
        return ResponseEntity.ok(Map.of("status", "UP", "service", "partner-service"));
    }
}
