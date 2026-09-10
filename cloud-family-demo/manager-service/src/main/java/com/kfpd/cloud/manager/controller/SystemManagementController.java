package com.kfpd.cloud.manager.controller;

import java.util.List;

import com.kfpd.cloud.manager.pojo.dto.IdListDTO;
import com.kfpd.cloud.manager.pojo.dto.SysMenuRequestDTO;
import com.kfpd.cloud.manager.pojo.dto.SysPermissionRequestDTO;
import com.kfpd.cloud.manager.pojo.dto.SysRoleRequestDTO;
import com.kfpd.cloud.manager.pojo.dto.SysUserAccessDTO;
import com.kfpd.cloud.manager.pojo.dto.SysUserRequestDTO;
import com.kfpd.cloud.manager.pojo.entity.SysMenu;
import com.kfpd.cloud.manager.pojo.entity.SysPermission;
import com.kfpd.cloud.manager.pojo.entity.SysRole;
import com.kfpd.cloud.manager.pojo.entity.SysUser;
import com.kfpd.cloud.manager.service.SystemManagementService;

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
@RequestMapping("/api/manager/system")
public class SystemManagementController {

    private final SystemManagementService systemManagementService;

    public SystemManagementController(SystemManagementService systemManagementService) {
        this.systemManagementService = systemManagementService;
    }

    @GetMapping("/users")
    public List<SysUser> users() {
        return systemManagementService.findUsers();
    }

    @GetMapping("/users/{id}")
    public SysUser user(@PathVariable Long id) {
        return systemManagementService.findUserById(id);
    }

    @GetMapping("/users/{id}/access")
    public SysUserAccessDTO userAccess(@PathVariable Long id) {
        return systemManagementService.findUserAccess(id);
    }

    @PostMapping("/users")
    public ResponseEntity<SysUser> createUser(@RequestBody SysUserRequestDTO request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(systemManagementService.createUser(request));
    }

    @PutMapping("/users/{id}")
    public SysUser updateUser(@PathVariable Long id, @RequestBody SysUserRequestDTO request) {
        return systemManagementService.updateUser(id, request);
    }

    @DeleteMapping("/users/{id}")
    public ResponseEntity<Void> deleteUser(@PathVariable Long id) {
        systemManagementService.deleteUser(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/users/{id}/roles")
    public List<SysRole> userRoles(@PathVariable Long id) {
        return systemManagementService.findUserRoles(id);
    }

    @PutMapping("/users/{id}/roles")
    public List<SysRole> replaceUserRoles(@PathVariable Long id, @RequestBody IdListDTO request) {
        return systemManagementService.replaceUserRoles(id, request);
    }

    @GetMapping("/roles")
    public List<SysRole> roles() {
        return systemManagementService.findRoles();
    }

    @GetMapping("/roles/{id}")
    public SysRole role(@PathVariable Long id) {
        return systemManagementService.findRoleById(id);
    }

    @PostMapping("/roles")
    public ResponseEntity<SysRole> createRole(@RequestBody SysRoleRequestDTO request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(systemManagementService.createRole(request));
    }

    @PutMapping("/roles/{id}")
    public SysRole updateRole(@PathVariable Long id, @RequestBody SysRoleRequestDTO request) {
        return systemManagementService.updateRole(id, request);
    }

    @DeleteMapping("/roles/{id}")
    public ResponseEntity<Void> deleteRole(@PathVariable Long id) {
        systemManagementService.deleteRole(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/roles/{id}/permissions")
    public List<SysPermission> rolePermissions(@PathVariable Long id) {
        return systemManagementService.findRolePermissions(id);
    }

    @PutMapping("/roles/{id}/permissions")
    public List<SysPermission> replaceRolePermissions(@PathVariable Long id, @RequestBody IdListDTO request) {
        return systemManagementService.replaceRolePermissions(id, request);
    }

    @GetMapping("/roles/{id}/menus")
    public List<SysMenu> roleMenus(@PathVariable Long id) {
        return systemManagementService.findRoleMenus(id);
    }

    @PutMapping("/roles/{id}/menus")
    public List<SysMenu> replaceRoleMenus(@PathVariable Long id, @RequestBody IdListDTO request) {
        return systemManagementService.replaceRoleMenus(id, request);
    }

    @GetMapping("/permissions")
    public List<SysPermission> permissions() {
        return systemManagementService.findPermissions();
    }

    @GetMapping("/permissions/{id}")
    public SysPermission permission(@PathVariable Long id) {
        return systemManagementService.findPermissionById(id);
    }

    @PostMapping("/permissions")
    public ResponseEntity<SysPermission> createPermission(@RequestBody SysPermissionRequestDTO request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(systemManagementService.createPermission(request));
    }

    @PutMapping("/permissions/{id}")
    public SysPermission updatePermission(@PathVariable Long id, @RequestBody SysPermissionRequestDTO request) {
        return systemManagementService.updatePermission(id, request);
    }

    @DeleteMapping("/permissions/{id}")
    public ResponseEntity<Void> deletePermission(@PathVariable Long id) {
        systemManagementService.deletePermission(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/menus")
    public List<SysMenu> menus() {
        return systemManagementService.findMenus();
    }

    @GetMapping("/menus/{id}")
    public SysMenu menu(@PathVariable Long id) {
        return systemManagementService.findMenuById(id);
    }

    @PostMapping("/menus")
    public ResponseEntity<SysMenu> createMenu(@RequestBody SysMenuRequestDTO request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(systemManagementService.createMenu(request));
    }

    @PutMapping("/menus/{id}")
    public SysMenu updateMenu(@PathVariable Long id, @RequestBody SysMenuRequestDTO request) {
        return systemManagementService.updateMenu(id, request);
    }

    @DeleteMapping("/menus/{id}")
    public ResponseEntity<Void> deleteMenu(@PathVariable Long id) {
        systemManagementService.deleteMenu(id);
        return ResponseEntity.noContent().build();
    }
}
