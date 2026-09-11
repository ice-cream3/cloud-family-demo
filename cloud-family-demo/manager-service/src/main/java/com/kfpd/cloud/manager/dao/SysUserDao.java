package com.kfpd.cloud.manager.dao;

import java.util.List;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.kfpd.cloud.manager.pojo.entity.SysMenu;
import com.kfpd.cloud.manager.pojo.entity.SysPermission;
import com.kfpd.cloud.manager.pojo.entity.SysRole;
import com.kfpd.cloud.manager.pojo.entity.SysUser;

import org.apache.ibatis.annotations.Param;

public interface SysUserDao extends BaseMapper<SysUser> {

    List<SysUser> findAll();

    SysUser findById(@Param("id") Long id);

    SysUser findByUsername(@Param("username") String username);

    int insert(SysUser user);

    int update(SysUser user);

    int deleteById(@Param("id") Long id);

    List<SysRole> findRolesByUserId(@Param("userId") Long userId);

    List<SysPermission> findPermissionsByUserId(@Param("userId") Long userId);

    List<SysMenu> findMenusByUserId(@Param("userId") Long userId);

    int deleteUserRoles(@Param("userId") Long userId);

    int insertUserRole(@Param("userId") Long userId, @Param("roleId") Long roleId);
}
