package com.kfpd.cloud.auth.dao;

import java.util.List;

import com.kfpd.cloud.auth.service.AuthLoginAccount;

import org.apache.ibatis.annotations.Param;

public interface AuthLoginAccountDao {

    AuthLoginAccount findByUsername(@Param("username") String username);

    List<String> findRolesByUsername(@Param("username") String username);

    List<String> findPermissionsByUsername(@Param("username") String username);
}
