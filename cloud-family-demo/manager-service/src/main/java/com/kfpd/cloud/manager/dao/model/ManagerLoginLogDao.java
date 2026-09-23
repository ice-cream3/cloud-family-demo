package com.kfpd.cloud.manager.dao.model;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.kfpd.cloud.manager.pojo.entity.ManagerLoginLog;

public interface ManagerLoginLogDao extends BaseMapper<ManagerLoginLog> {

    int insertLoginLog(ManagerLoginLog log);
}
