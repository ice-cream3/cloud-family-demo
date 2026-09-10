package com.kfpd.cloud.manager.dao;

import java.util.List;

import com.kfpd.cloud.manager.pojo.entity.SysPermission;

import org.apache.ibatis.annotations.Param;

public interface SysPermissionDao {

    List<SysPermission> findAll();

    SysPermission findById(@Param("id") Long id);

    int insert(SysPermission permission);

    int update(SysPermission permission);

    int deleteById(@Param("id") Long id);
}
