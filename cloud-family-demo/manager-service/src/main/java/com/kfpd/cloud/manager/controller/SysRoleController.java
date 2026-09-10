package com.kfpd.cloud.manager.controller;

import java.util.List;

import com.kfpd.cloud.manager.pojo.vo.IdListVO;
import com.kfpd.cloud.manager.pojo.vo.SysRoleRequestVO;
import com.kfpd.cloud.manager.pojo.entity.SysMenu;
import com.kfpd.cloud.manager.pojo.entity.SysPermission;
import com.kfpd.cloud.manager.pojo.entity.SysRole;
import com.kfpd.cloud.manager.service.SysRoleService;

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
@RequestMapping("/api/manager/system/roles")
public class SysRoleController {

    private final SysRoleService roleService;

    public SysRoleController(SysRoleService roleService) {
        this.roleService = roleService;
    }

    @GetMapping
    public List<SysRole> roles() {
        return roleService.findRoles();
    }

    @GetMapping("/{id}")
    public SysRole role(@PathVariable Long id) {
        return roleService.findRoleById(id);
    }

    @PostMapping
    public ResponseEntity<SysRole> createRole(@RequestBody SysRoleRequestVO request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(roleService.createRole(request));
    }

    @PutMapping("/{id}")
    public SysRole updateRole(@PathVariable Long id, @RequestBody SysRoleRequestVO request) {
        return roleService.updateRole(id, request);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteRole(@PathVariable Long id) {
        roleService.deleteRole(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}/permissions")
    public List<SysPermission> rolePermissions(@PathVariable Long id) {
        return roleService.findRolePermissions(id);
    }

    @PutMapping("/{id}/permissions")
    public List<SysPermission> replaceRolePermissions(@PathVariable Long id, @RequestBody IdListVO request) {
        return roleService.replaceRolePermissions(id, request);
    }

    @GetMapping("/{id}/menus")
    public List<SysMenu> roleMenus(@PathVariable Long id) {
        return roleService.findRoleMenus(id);
    }

    @PutMapping("/{id}/menus")
    public List<SysMenu> replaceRoleMenus(@PathVariable Long id, @RequestBody IdListVO request) {
        return roleService.replaceRoleMenus(id, request);
    }
}
