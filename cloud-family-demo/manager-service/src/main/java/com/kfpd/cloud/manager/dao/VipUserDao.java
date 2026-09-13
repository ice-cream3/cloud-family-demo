package com.kfpd.cloud.manager.dao;

import java.util.List;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.kfpd.cloud.manager.pojo.entity.VipUser;

import org.apache.ibatis.annotations.Param;

public interface VipUserDao extends BaseMapper<VipUser> {

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

    int insert(VipUser vipUser);

    int update(VipUser vipUser);

    int deleteById(@Param("id") Long id);
}
