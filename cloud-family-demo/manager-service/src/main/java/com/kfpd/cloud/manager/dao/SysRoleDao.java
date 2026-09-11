package com.kfpd.cloud.manager.dao;

import java.util.List;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.kfpd.cloud.manager.pojo.entity.SysMenu;
import com.kfpd.cloud.manager.pojo.entity.SysPermission;
import com.kfpd.cloud.manager.pojo.entity.SysRole;

import org.apache.ibatis.annotations.Param;

public interface SysRoleDao extends BaseMapper<SysRole> {

    List<SysRole> findAll();

    SysRole findById(@Param("id") Long id);

    int insert(SysRole role);

    int update(SysRole role);

    int deleteById(@Param("id") Long id);

    List<SysPermission> findPermissionsByRoleId(@Param("roleId") Long roleId);

    int deleteRolePermissions(@Param("roleId") Long roleId);

    int insertRolePermission(@Param("roleId") Long roleId, @Param("permissionId") Long permissionId);

    List<SysMenu> findMenusByRoleId(@Param("roleId") Long roleId);

    int deleteRoleMenus(@Param("roleId") Long roleId);

    int insertRoleMenu(@Param("roleId") Long roleId, @Param("menuId") Long menuId);
}
