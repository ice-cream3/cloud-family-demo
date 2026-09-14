package com.kfpd.cloud.auth.dao.model;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.kfpd.cloud.auth.pojo.entity.PartnerLoginLog;

import org.apache.ibatis.annotations.Param;

public interface PartnerLoginLogDao extends BaseMapper<PartnerLoginLog> {

    int insertLoginLog(@Param("log") PartnerLoginLog log);
}
