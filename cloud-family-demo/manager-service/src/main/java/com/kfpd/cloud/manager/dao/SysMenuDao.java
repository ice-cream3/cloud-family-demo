package com.kfpd.cloud.manager.dao;

import java.util.List;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.kfpd.cloud.manager.pojo.entity.SysMenu;

import org.apache.ibatis.annotations.Param;

public interface SysMenuDao extends BaseMapper<SysMenu> {

    List<SysMenu> findAll();

    SysMenu findById(@Param("id") Long id);

    int insert(SysMenu menu);

    int update(SysMenu menu);

    int deleteById(@Param("id") Long id);
}
