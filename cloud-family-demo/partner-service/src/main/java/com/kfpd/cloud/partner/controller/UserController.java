package com.kfpd.cloud.partner.controller;

import java.util.Map;

import com.kfpd.cloud.common.web.ApiResponse;
import com.kfpd.cloud.common.web.GatewayHeaders;
import com.kfpd.cloud.partner.pojo.vo.PageVO;
import com.kfpd.cloud.partner.pojo.vo.UserProfileVO;
import com.kfpd.cloud.partner.pojo.vo.VipUserPageQueryVO;
import com.kfpd.cloud.partner.pojo.vo.VipUserRequestVO;
import com.kfpd.cloud.partner.pojo.vo.VipUserVO;
import com.kfpd.cloud.partner.service.UserService;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
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

    @PostMapping({"/me", "/my"})
    public ApiResponse<UserProfileVO> currentUser(
            @RequestHeader(value = GatewayHeaders.USER_NAME, defaultValue = "anonymous") String username,
            @RequestHeader(value = GatewayHeaders.USER_ROLES, required = false) String roles
    ) {
        return ApiResponse.success(userService.currentUser(username, roles));
    }

    @PostMapping("/vip-users/page")
    public ApiResponse<PageVO<VipUserVO>> vipUsers(@ModelAttribute VipUserPageQueryVO query) {
        return ApiResponse.success(userService.findVipUsers(query));
    }

    @PostMapping("/vip-users/detail/{id}")
    public ApiResponse<VipUserVO> vipUser(@PathVariable Long id) {
        return ApiResponse.success(userService.findVipUserById(id));
    }

    @PostMapping("/vip-users")
    public ResponseEntity<ApiResponse<VipUserVO>> createVipUser(@RequestBody VipUserRequestVO request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(userService.createVipUser(request)));
    }

    @PostMapping("/vip-users/update/{id}")
    public ApiResponse<VipUserVO> updateVipUser(@PathVariable Long id, @RequestBody VipUserRequestVO request) {
        return ApiResponse.success(userService.updateVipUser(id, request));
    }

    @PostMapping("/vip-users/delete/{id}")
    public ApiResponse<Void> deleteVipUser(@PathVariable Long id) {
        userService.deleteVipUser(id);
        return ApiResponse.success();
    }

    @PostMapping("/health")
    public ApiResponse<Map<String, String>> health() {
        return ApiResponse.success(Map.of("status", "UP", "service", "partner-service"));
    }
}
