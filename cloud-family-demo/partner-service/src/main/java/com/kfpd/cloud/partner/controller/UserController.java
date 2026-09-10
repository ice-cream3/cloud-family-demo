package com.kfpd.cloud.partner.controller;

import java.util.Map;

import com.kfpd.cloud.common.web.GatewayHeaders;
import com.kfpd.cloud.partner.pojo.vo.PageVO;
import com.kfpd.cloud.partner.pojo.vo.UserProfileVO;
import com.kfpd.cloud.partner.pojo.vo.VipUserPageQueryVO;
import com.kfpd.cloud.partner.pojo.vo.VipUserRequestVO;
import com.kfpd.cloud.partner.pojo.vo.VipUserVO;
import com.kfpd.cloud.partner.service.UserService;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping({"/me", "/my"})
    public UserProfileVO currentUser(
            @RequestHeader(value = GatewayHeaders.USER_NAME, defaultValue = "anonymous") String username,
            @RequestHeader(value = GatewayHeaders.USER_ROLES, required = false) String roles
    ) {
        return userService.currentUser(username, roles);
    }

    @GetMapping("/vip-users")
    public PageVO<VipUserVO> vipUsers(@ModelAttribute VipUserPageQueryVO query) {
        return userService.findVipUsers(query);
    }

    @GetMapping("/vip-users/{id}")
    public VipUserVO vipUser(@PathVariable Long id) {
        return userService.findVipUserById(id);
    }

    @PostMapping("/vip-users")
    public ResponseEntity<VipUserVO> createVipUser(@RequestBody VipUserRequestVO request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(userService.createVipUser(request));
    }

    @PutMapping("/vip-users/{id}")
    public VipUserVO updateVipUser(@PathVariable Long id, @RequestBody VipUserRequestVO request) {
        return userService.updateVipUser(id, request);
    }

    @DeleteMapping("/vip-users/{id}")
    public ResponseEntity<Void> deleteVipUser(@PathVariable Long id) {
        userService.deleteVipUser(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> health() {
        return ResponseEntity.ok(Map.of("status", "UP", "service", "partner-service"));
    }
}
