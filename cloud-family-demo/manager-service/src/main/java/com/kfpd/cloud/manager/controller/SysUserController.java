package com.kfpd.cloud.manager.controller;

import java.util.List;

import com.kfpd.cloud.common.web.ApiResponse;
import com.kfpd.cloud.manager.pojo.vo.IdListVO;
import com.kfpd.cloud.manager.pojo.dto.SysUserAccessDTO;
import com.kfpd.cloud.manager.pojo.vo.PageQueryVO;
import com.kfpd.cloud.manager.pojo.vo.PageVO;
import com.kfpd.cloud.manager.pojo.vo.PasswordResetRequestVO;
import com.kfpd.cloud.manager.pojo.vo.SysUserRequestVO;
import com.kfpd.cloud.manager.pojo.vo.SysUserVO;
import com.kfpd.cloud.manager.pojo.entity.SysRole;
import com.kfpd.cloud.manager.service.SysUserService;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/manager/system")
public class SysUserController {

    private final SysUserService userService;

    public SysUserController(SysUserService userService) {
        this.userService = userService;
    }

    @PostMapping("/users/page")
    public ApiResponse<PageVO<SysUserVO>> users(@ModelAttribute PageQueryVO query) {
        return ApiResponse.success(userService.findUsers(query));
    }

    @PostMapping("/users/detail/{id}")
    public ApiResponse<SysUserVO> user(@PathVariable Long id) {
        return ApiResponse.success(userService.findUserById(id));
    }

    @PostMapping("/users/access/{id}")
    public ApiResponse<SysUserAccessDTO> userAccess(@PathVariable Long id) {
        return ApiResponse.success(userService.findUserAccess(id));
    }

    @PostMapping("/users")
    public ResponseEntity<ApiResponse<SysUserVO>> createUser(@RequestBody(required = false) SysUserRequestVO request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(userService.createUser(request)));
    }

    @PostMapping("/users/update/{id}")
    public ApiResponse<SysUserVO> updateUser(@PathVariable Long id, @RequestBody(required = false) SysUserRequestVO request) {
        return ApiResponse.success(userService.updateUser(id, request));
    }

    @PostMapping("/users/password/reset/{id}")
    public ApiResponse<SysUserVO> resetUserPassword(@PathVariable Long id, @RequestBody(required = false) PasswordResetRequestVO request) {
        return ApiResponse.success(userService.resetUserPassword(id, request));
    }

    @PostMapping("/users/delete/{id}")
    public ApiResponse<Void> deleteUser(@PathVariable Long id) {
        userService.deleteUser(id);
        return ApiResponse.success();
    }

    @PostMapping("/users/roles/{id}")
    public ApiResponse<List<SysRole>> userRoles(@PathVariable Long id) {
        return ApiResponse.success(userService.findUserRoles(id));
    }

    @PostMapping("/users/roles/replace/{id}")
    public ApiResponse<List<SysRole>> replaceUserRoles(@PathVariable Long id, @RequestBody IdListVO request) {
        return ApiResponse.success(userService.replaceUserRoles(id, request));
    }
}
