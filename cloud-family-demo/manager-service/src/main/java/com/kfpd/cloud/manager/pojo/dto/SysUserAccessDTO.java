package com.kfpd.cloud.manager.pojo.dto;

import java.util.List;

import com.kfpd.cloud.manager.pojo.entity.SysMenu;
import com.kfpd.cloud.manager.pojo.entity.SysPermission;
import com.kfpd.cloud.manager.pojo.entity.SysRole;
import com.kfpd.cloud.manager.pojo.entity.SysUser;

public record SysUserAccessDTO(
        SysUser user,
        List<SysRole> roles,
        List<SysPermission> permissions,
        List<SysMenu> menus
) {
}
