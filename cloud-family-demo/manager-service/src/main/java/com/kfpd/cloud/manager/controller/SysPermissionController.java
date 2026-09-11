package com.kfpd.cloud.manager.controller;

import java.util.List;

import com.kfpd.cloud.common.web.ApiResponse;
import com.kfpd.cloud.manager.pojo.vo.SysPermissionRequestVO;
import com.kfpd.cloud.manager.pojo.entity.SysPermission;
import com.kfpd.cloud.manager.service.SysPermissionService;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/manager/system/permissions")
public class SysPermissionController {

    private final SysPermissionService permissionService;

    public SysPermissionController(SysPermissionService permissionService) {
        this.permissionService = permissionService;
    }

    @GetMapping
    public ApiResponse<List<SysPermission>> permissions() {
        return ApiResponse.success(permissionService.findPermissions());
    }

    @GetMapping("/{id}")
    public ApiResponse<SysPermission> permission(@PathVariable Long id) {
        return ApiResponse.success(permissionService.findPermissionById(id));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<SysPermission>> createPermission(@RequestBody SysPermissionRequestVO request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(permissionService.createPermission(request)));
    }

    @PutMapping("/{id}")
    public ApiResponse<SysPermission> updatePermission(@PathVariable Long id, @RequestBody SysPermissionRequestVO request) {
        return ApiResponse.success(permissionService.updatePermission(id, request));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> deletePermission(@PathVariable Long id) {
        permissionService.deletePermission(id);
        return ApiResponse.success();
    }
}
