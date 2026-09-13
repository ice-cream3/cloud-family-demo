package com.kfpd.cloud.auth.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.kfpd.cloud.auth.pojo.entity.ManagerLoginLog;

import org.apache.ibatis.annotations.Param;

public interface ManagerLoginLogDao extends BaseMapper<ManagerLoginLog> {

    int insertLoginLog(@Param("log") ManagerLoginLog log);
}
