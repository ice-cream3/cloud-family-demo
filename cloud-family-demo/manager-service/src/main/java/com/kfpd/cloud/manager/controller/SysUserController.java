package com.kfpd.cloud.manager.controller;

import java.util.List;

import com.kfpd.cloud.common.web.ApiResponse;
import com.kfpd.cloud.manager.pojo.vo.IdListVO;
import com.kfpd.cloud.manager.pojo.dto.SysUserAccessDTO;
import com.kfpd.cloud.manager.pojo.vo.PageQueryVO;
import com.kfpd.cloud.manager.pojo.vo.PageVO;
import com.kfpd.cloud.manager.pojo.vo.SysUserRequestVO;
import com.kfpd.cloud.manager.pojo.entity.SysRole;
import com.kfpd.cloud.manager.pojo.entity.SysUser;
import com.kfpd.cloud.manager.service.SysUserService;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/manager/system/users")
public class SysUserController {

    private final SysUserService userService;

    public SysUserController(SysUserService userService) {
        this.userService = userService;
    }

    @GetMapping
    public ApiResponse<PageVO<SysUser>> users(@ModelAttribute PageQueryVO query) {
        return ApiResponse.success(userService.findUsers(query));
    }

    @GetMapping("/{id}")
    public ApiResponse<SysUser> user(@PathVariable Long id) {
        return ApiResponse.success(userService.findUserById(id));
    }

    @GetMapping("/{id}/access")
    public ApiResponse<SysUserAccessDTO> userAccess(@PathVariable Long id) {
        return ApiResponse.success(userService.findUserAccess(id));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<SysUser>> createUser(@RequestBody SysUserRequestVO request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(userService.createUser(request)));
    }

    @PutMapping("/{id}")
    public ApiResponse<SysUser> updateUser(@PathVariable Long id, @RequestBody SysUserRequestVO request) {
        return ApiResponse.success(userService.updateUser(id, request));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> deleteUser(@PathVariable Long id) {
        userService.deleteUser(id);
        return ApiResponse.success();
    }

    @GetMapping("/{id}/roles")
    public ApiResponse<List<SysRole>> userRoles(@PathVariable Long id) {
        return ApiResponse.success(userService.findUserRoles(id));
    }

    @PutMapping("/{id}/roles")
    public ApiResponse<List<SysRole>> replaceUserRoles(@PathVariable Long id, @RequestBody IdListVO request) {
        return ApiResponse.success(userService.replaceUserRoles(id, request));
    }
}
