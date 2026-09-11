package com.kfpd.cloud.manager.service.impl;

import java.util.List;
import java.util.Optional;

import com.kfpd.cloud.common.datasource.MultiDataSourceNames;
import com.kfpd.cloud.manager.dao.SysPermissionDao;
import com.kfpd.cloud.manager.pojo.vo.SysPermissionRequestVO;
import com.kfpd.cloud.manager.pojo.entity.SysPermission;
import com.kfpd.cloud.manager.service.SysPermissionService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SysPermissionServiceImpl implements SysPermissionService {

    private static final Logger log = LoggerFactory.getLogger(SysPermissionServiceImpl.class);

    private final SysPermissionDao permissionDao;

    public SysPermissionServiceImpl(SysPermissionDao permissionDao) {
        this.permissionDao = permissionDao;
    }

    @Override
    public List<SysPermission> findPermissions() {
        return permissionDao.findAll();
    }

    @Override
    public SysPermission findPermissionById(Long id) {
        return Optional.ofNullable(permissionDao.findById(id)).orElseThrow(() -> {
            log.warn("Permission service exception: action=findPermissionById, id={}, message=Permission not found", id);
            return SystemManagementSupport.notFound("Permission not found");
        });
    }

    @Override
    @Transactional(transactionManager = MultiDataSourceNames.FA_CLOUD_TRANSACTION_MANAGER)
    public SysPermission createPermission(SysPermissionRequestVO request) {
        SystemManagementSupport.requireText(request.permissionCode(), "permissionCode is required");
        SystemManagementSupport.requireText(request.permissionName(), "permissionName is required");
        SysPermission permission = new SysPermission();
        apply(permission, request);
        permissionDao.insert(permission);
        return findPermissionById(permission.getId());
    }

    @Override
    @Transactional(transactionManager = MultiDataSourceNames.FA_CLOUD_TRANSACTION_MANAGER)
    public SysPermission updatePermission(Long id, SysPermissionRequestVO request) {
        SystemManagementSupport.requireText(request.permissionCode(), "permissionCode is required");
        SystemManagementSupport.requireText(request.permissionName(), "permissionName is required");
        SysPermission permission = new SysPermission();
        permission.setId(id);
        apply(permission, request);
        if (permissionDao.update(permission) == 0) {
            log.warn("Permission service exception: action=updatePermission, id={}, message=Permission not found", id);
            throw SystemManagementSupport.notFound("Permission not found");
        }
        return findPermissionById(id);
    }

    @Override
    @Transactional(transactionManager = MultiDataSourceNames.FA_CLOUD_TRANSACTION_MANAGER)
    public void deletePermission(Long id) {
        if (permissionDao.deleteById(id) == 0) {
            log.warn("Permission service exception: action=deletePermission, id={}, message=Permission not found", id);
            throw SystemManagementSupport.notFound("Permission not found");
        }
    }

    private void apply(SysPermission permission, SysPermissionRequestVO request) {
        permission.setPermissionCode(request.permissionCode());
        permission.setPermissionName(request.permissionName());
        permission.setDescription(request.description());
        permission.setStatus(SystemManagementSupport.defaultStatus(request.status()));
    }
}
