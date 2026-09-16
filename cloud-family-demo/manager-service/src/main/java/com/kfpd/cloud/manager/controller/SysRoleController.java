package com.kfpd.cloud.manager.controller;

import java.util.List;

import com.kfpd.cloud.common.web.ApiResponse;
import com.kfpd.cloud.manager.pojo.vo.IdListVO;
import com.kfpd.cloud.manager.pojo.vo.PageQueryVO;
import com.kfpd.cloud.manager.pojo.vo.PageVO;
import com.kfpd.cloud.manager.pojo.vo.SysRoleRequestVO;
import com.kfpd.cloud.manager.pojo.entity.SysMenu;
import com.kfpd.cloud.manager.pojo.entity.SysPermission;
import com.kfpd.cloud.manager.pojo.entity.SysRole;
import com.kfpd.cloud.manager.service.SysRoleService;

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
public class SysRoleController {

    private final SysRoleService roleService;

    public SysRoleController(SysRoleService roleService) {
        this.roleService = roleService;
    }

    @PostMapping("/roles/page")
    public ApiResponse<PageVO<SysRole>> roles(@ModelAttribute PageQueryVO query) {
        return ApiResponse.success(roleService.findRoles(query));
    }

    @PostMapping("/roles/detail/{id}")
    public ApiResponse<SysRole> role(@PathVariable Long id) {
        return ApiResponse.success(roleService.findRoleById(id));
    }

    @PostMapping("/roles")
    public ResponseEntity<ApiResponse<SysRole>> createRole(@RequestBody SysRoleRequestVO request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(roleService.createRole(request)));
    }

    @PostMapping("/roles/update/{id}")
    public ApiResponse<SysRole> updateRole(@PathVariable Long id, @RequestBody SysRoleRequestVO request) {
        return ApiResponse.success(roleService.updateRole(id, request));
    }

    @PostMapping("/roles/delete/{id}")
    public ApiResponse<Void> deleteRole(@PathVariable Long id) {
        roleService.deleteRole(id);
        return ApiResponse.success();
    }

    @PostMapping("/roles/permissions/{id}")
    public ApiResponse<List<SysPermission>> rolePermissions(@PathVariable Long id) {
        return ApiResponse.success(roleService.findRolePermissions(id));
    }

    @PostMapping("/roles/permissions/replace/{id}")
    public ApiResponse<List<SysPermission>> replaceRolePermissions(@PathVariable Long id, @RequestBody IdListVO request) {
        return ApiResponse.success(roleService.replaceRolePermissions(id, request));
    }

    @PostMapping("/roles/menus/{id}")
    public ApiResponse<List<SysMenu>> roleMenus(@PathVariable Long id) {
        return ApiResponse.success(roleService.findRoleMenus(id));
    }

    @PostMapping("/roles/menus/replace/{id}")
    public ApiResponse<List<SysMenu>> replaceRoleMenus(@PathVariable Long id, @RequestBody IdListVO request) {
        return ApiResponse.success(roleService.replaceRoleMenus(id, request));
    }
}
