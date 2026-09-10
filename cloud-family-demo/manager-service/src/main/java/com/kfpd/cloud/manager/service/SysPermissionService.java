package com.kfpd.cloud.manager.service;

import java.util.List;

import com.kfpd.cloud.manager.pojo.vo.SysPermissionRequestVO;
import com.kfpd.cloud.manager.pojo.entity.SysPermission;

public interface SysPermissionService {

    List<SysPermission> findPermissions();

    SysPermission findPermissionById(Long id);

    SysPermission createPermission(SysPermissionRequestVO request);

    SysPermission updatePermission(Long id, SysPermissionRequestVO request);

    void deletePermission(Long id);
}
