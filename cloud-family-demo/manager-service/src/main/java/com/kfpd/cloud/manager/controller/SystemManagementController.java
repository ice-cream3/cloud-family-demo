package com.kfpd.cloud.manager.controller;

import java.util.List;

import com.kfpd.cloud.manager.pojo.vo.SysMenuRequestVO;
import com.kfpd.cloud.manager.pojo.entity.SysMenu;
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
@RequestMapping("/api/manager/system")
public class SystemManagementController {

    private final SysRoleService roleService;

    public SystemManagementController(SysRoleService roleService) {
        this.roleService = roleService;
    }

    @GetMapping("/menus")
    public List<SysMenu> menus() {
        return roleService.findMenus();
    }

    @GetMapping("/menus/{id}")
    public SysMenu menu(@PathVariable Long id) {
        return roleService.findMenuById(id);
    }

    @PostMapping("/menus")
    public ResponseEntity<SysMenu> createMenu(@RequestBody SysMenuRequestVO request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(roleService.createMenu(request));
    }

    @PutMapping("/menus/{id}")
    public SysMenu updateMenu(@PathVariable Long id, @RequestBody SysMenuRequestVO request) {
        return roleService.updateMenu(id, request);
    }

    @DeleteMapping("/menus/{id}")
    public ResponseEntity<Void> deleteMenu(@PathVariable Long id) {
        roleService.deleteMenu(id);
        return ResponseEntity.noContent().build();
    }
}
