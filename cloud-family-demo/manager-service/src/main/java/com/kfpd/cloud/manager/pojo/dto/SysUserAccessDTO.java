package com.kfpd.cloud.manager.pojo.dto;

import java.util.List;

import com.kfpd.cloud.manager.pojo.entity.SysMenu;
import com.kfpd.cloud.manager.pojo.entity.SysPermission;
import com.kfpd.cloud.manager.pojo.entity.SysRole;
import com.kfpd.cloud.manager.pojo.vo.SysUserVO;

public record SysUserAccessDTO(
        SysUserVO user,
        List<SysRole> roles,
        List<SysPermission> permissions,
        List<SysMenu> menus
) {
}
