package com.kfpd.cloud.partner.dao;

import com.kfpd.cloud.partner.pojo.VipUser;

import org.apache.ibatis.annotations.Param;

public interface VipUserDao {

    VipUser findByUsername(@Param("username") String username);
}
