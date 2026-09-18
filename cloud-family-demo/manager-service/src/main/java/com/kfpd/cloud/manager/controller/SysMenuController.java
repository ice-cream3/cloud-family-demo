package com.kfpd.cloud.manager.controller;

import java.util.List;

import com.kfpd.cloud.common.web.ApiResponse;
import com.kfpd.cloud.common.web.GatewayHeaders;
import com.kfpd.cloud.manager.pojo.entity.SysMenu;
import com.kfpd.cloud.manager.pojo.vo.PageQueryVO;
import com.kfpd.cloud.manager.pojo.vo.PageVO;
import com.kfpd.cloud.manager.pojo.vo.SysMenuRequestVO;
import com.kfpd.cloud.manager.pojo.vo.SysMenuTreeVO;
import com.kfpd.cloud.manager.service.SysRoleService;
import com.kfpd.cloud.manager.service.SysUserService;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/manager/system")
public class SysMenuController {

    private final SysRoleService roleService;
    private final SysUserService userService;

    public SysMenuController(SysRoleService roleService, SysUserService userService) {
        this.roleService = roleService;
        this.userService = userService;
    }

    @PostMapping("/menus/page")
    public ApiResponse<PageVO<SysMenu>> menus(@ModelAttribute PageQueryVO query) {
        return ApiResponse.success(roleService.findMenus(query));
    }

    @PostMapping("/menus/tree")
    public ApiResponse<List<SysMenuTreeVO>> menuTree() {
        return ApiResponse.success(roleService.findMenuTree());
    }

    @PostMapping("/menus/current-user/tree")
    public ApiResponse<List<SysMenuTreeVO>> currentUserMenuTree(
            @RequestHeader(value = GatewayHeaders.USER_NAME, defaultValue = "") String username) {
        return ApiResponse.success(userService.findUserMenuTree(username));
    }

    @PostMapping("/menus/detail/{id}")
    public ApiResponse<SysMenu> menu(@PathVariable Long id) {
        return ApiResponse.success(roleService.findMenuById(id));
    }

    @PostMapping("/menus")
    public ResponseEntity<ApiResponse<SysMenu>> createMenu(@RequestBody SysMenuRequestVO request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(roleService.createMenu(request)));
    }

    @PostMapping("/menus/update/{id}")
    public ApiResponse<SysMenu> updateMenu(@PathVariable Long id, @RequestBody SysMenuRequestVO request) {
        return ApiResponse.success(roleService.updateMenu(id, request));
    }

    @PostMapping("/menus/delete/{id}")
    public ApiResponse<Void> deleteMenu(@PathVariable Long id) {
        roleService.deleteMenu(id);
        return ApiResponse.success();
    }
}
