package com.kfpd.cloud.auth.dao.cloud;

import java.util.List;

import org.apache.ibatis.annotations.Param;

public interface OAuth2AuthorizationDao {

    List<String> findIdsByPrincipalName(@Param("username") String username);
}
