package com.kfpd.cloud.manager.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.kfpd.cloud.manager.pojo.entity.OperationLog;

public interface OperationLogDao extends BaseMapper<OperationLog> {

    int insert(OperationLog operationLog);
}
