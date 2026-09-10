package com.kfpd.cloud.manager.controller;

import java.util.List;

import com.kfpd.cloud.manager.pojo.vo.IdListVO;
import com.kfpd.cloud.manager.pojo.dto.SysUserAccessDTO;
import com.kfpd.cloud.manager.pojo.vo.SysUserRequestVO;
import com.kfpd.cloud.manager.pojo.entity.SysRole;
import com.kfpd.cloud.manager.pojo.entity.SysUser;
import com.kfpd.cloud.manager.service.SysUserService;

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
@RequestMapping("/api/manager/system/users")
public class SysUserController {

    private final SysUserService userService;

    public SysUserController(SysUserService userService) {
        this.userService = userService;
    }

    @GetMapping
    public List<SysUser> users() {
        return userService.findUsers();
    }

    @GetMapping("/{id}")
    public SysUser user(@PathVariable Long id) {
        return userService.findUserById(id);
    }

    @GetMapping("/{id}/access")
    public SysUserAccessDTO userAccess(@PathVariable Long id) {
        return userService.findUserAccess(id);
    }

    @PostMapping
    public ResponseEntity<SysUser> createUser(@RequestBody SysUserRequestVO request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(userService.createUser(request));
    }

    @PutMapping("/{id}")
    public SysUser updateUser(@PathVariable Long id, @RequestBody SysUserRequestVO request) {
        return userService.updateUser(id, request);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteUser(@PathVariable Long id) {
        userService.deleteUser(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}/roles")
    public List<SysRole> userRoles(@PathVariable Long id) {
        return userService.findUserRoles(id);
    }

    @PutMapping("/{id}/roles")
    public List<SysRole> replaceUserRoles(@PathVariable Long id, @RequestBody IdListVO request) {
        return userService.replaceUserRoles(id, request);
    }
}
