package com.kfpd.cloud.manager.dao.model;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.kfpd.cloud.manager.pojo.entity.PartnerLoginLog;

public interface PartnerLoginLogDao extends BaseMapper<PartnerLoginLog> {

    int insertLoginLog(PartnerLoginLog log);
}
