package com.kfpd.cloud.manager.controller;

import java.util.List;

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
    public List<SysPermission> permissions() {
        return permissionService.findPermissions();
    }

    @GetMapping("/{id}")
    public SysPermission permission(@PathVariable Long id) {
        return permissionService.findPermissionById(id);
    }

    @PostMapping
    public ResponseEntity<SysPermission> createPermission(@RequestBody SysPermissionRequestVO request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(permissionService.createPermission(request));
    }

    @PutMapping("/{id}")
    public SysPermission updatePermission(@PathVariable Long id, @RequestBody SysPermissionRequestVO request) {
        return permissionService.updatePermission(id, request);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletePermission(@PathVariable Long id) {
        permissionService.deletePermission(id);
        return ResponseEntity.noContent().build();
    }
}
