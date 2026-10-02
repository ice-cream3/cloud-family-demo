package com.kfpd.cloud.auth.dao.cloud;

import java.util.List;

import com.kfpd.cloud.auth.pojo.dto.AuthLoginAccount;

import org.apache.ibatis.annotations.Param;

public interface AuthLoginAccountDao {

    AuthLoginAccount findApiByUsername(@Param("username") String username);

    long countApiByUsername(@Param("username") String username);

    int insertApiUser(@Param("username") String username,
                      @Param("passwordHash") String passwordHash,
                      @Param("displayName") String displayName,
                      @Param("email") String email,
                      @Param("phone") String phone);

    /**
     * 查询用户:关联角色和权限,留意功能
     * @param username
     * @return
     */
    AuthLoginAccount findManagerByUsername(@Param("username") String username);

    List<String> findRolesByUsername(@Param("username") String username);

    List<String> findPermissionsByUsername(@Param("username") String username);
}
