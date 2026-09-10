package com.kfpd.cloud.partner.dao;

import java.util.List;

import com.kfpd.cloud.partner.pojo.entity.VipUser;

import org.apache.ibatis.annotations.Param;

public interface VipUserDao {

    List<VipUser> findPage(@Param("username") String username,
                           @Param("displayName") String displayName,
                           @Param("vipLevel") String vipLevel,
                           @Param("status") String status,
                           @Param("offset") int offset,
                           @Param("pageSize") int pageSize);

    long count(@Param("username") String username,
               @Param("displayName") String displayName,
               @Param("vipLevel") String vipLevel,
               @Param("status") String status);

    VipUser findById(@Param("id") Long id);

    VipUser findByUsername(@Param("username") String username);

    int insert(VipUser vipUser);

    int update(VipUser vipUser);

    int deleteById(@Param("id") Long id);
}
