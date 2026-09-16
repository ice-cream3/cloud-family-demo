package com.kfpd.cloud.manager.controller;

import com.kfpd.cloud.common.web.ApiResponse;
import com.kfpd.cloud.manager.pojo.vo.PageQueryVO;
import com.kfpd.cloud.manager.pojo.vo.PageVO;
import com.kfpd.cloud.manager.pojo.vo.SysPermissionRequestVO;
import com.kfpd.cloud.manager.pojo.entity.SysPermission;
import com.kfpd.cloud.manager.service.SysPermissionService;

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
public class SysPermissionController {

    private final SysPermissionService permissionService;

    public SysPermissionController(SysPermissionService permissionService) {
        this.permissionService = permissionService;
    }

    @PostMapping("/permissions/page")
    public ApiResponse<PageVO<SysPermission>> permissions(@ModelAttribute PageQueryVO query) {
        return ApiResponse.success(permissionService.findPermissions(query));
    }

    @PostMapping("/permissions/detail/{id}")
    public ApiResponse<SysPermission> permission(@PathVariable Long id) {
        return ApiResponse.success(permissionService.findPermissionById(id));
    }

    @PostMapping("/permissions")
    public ResponseEntity<ApiResponse<SysPermission>> createPermission(@RequestBody SysPermissionRequestVO request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(permissionService.createPermission(request)));
    }

    @PostMapping("/permissions/update/{id}")
    public ApiResponse<SysPermission> updatePermission(@PathVariable Long id, @RequestBody SysPermissionRequestVO request) {
        return ApiResponse.success(permissionService.updatePermission(id, request));
    }

    @PostMapping("/permissions/delete/{id}")
    public ApiResponse<Void> deletePermission(@PathVariable Long id) {
        permissionService.deletePermission(id);
        return ApiResponse.success();
    }
}
