package com.kfpd.cloud.manager.service.impl;

import java.util.List;
import java.util.Optional;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.kfpd.cloud.common.config.datasource.MultiDataSourceNames;
import com.kfpd.cloud.manager.dao.SysPermissionDao;
import com.kfpd.cloud.manager.pojo.vo.PageQueryVO;
import com.kfpd.cloud.manager.pojo.vo.PageVO;
import com.kfpd.cloud.manager.pojo.vo.SysPermissionRequestVO;
import com.kfpd.cloud.manager.pojo.entity.SysPermission;
import com.kfpd.cloud.manager.service.OperationLogService;
import com.kfpd.cloud.manager.service.SysPermissionService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SysPermissionServiceImpl implements SysPermissionService {

    private static final Logger log = LoggerFactory.getLogger(SysPermissionServiceImpl.class);
    private static final String MODULE_SYSTEM = "SYSTEM";
    private static final String BUSINESS_PERMISSION = "SYS_PERMISSION";

    private final SysPermissionDao permissionDao;
    private final OperationLogService operationLogService;

    public SysPermissionServiceImpl(SysPermissionDao permissionDao, OperationLogService operationLogService) {
        this.permissionDao = permissionDao;
        this.operationLogService = operationLogService;
    }

    @Override
    public List<SysPermission> findPermissions() {
        return permissionDao.findAll();
    }

    @Override
    public PageVO<SysPermission> findPermissions(PageQueryVO query) {
        int normalizedPageNum = SystemManagementSupport.pageNum(query);
        int normalizedPageSize = SystemManagementSupport.pageSize(query);
        Page<SysPermission> page = permissionDao.selectPage(
                new Page<>(normalizedPageNum, normalizedPageSize),
                Wrappers.lambdaQuery(SysPermission.class).orderByDesc(SysPermission::getId)
        );
        return SystemManagementSupport.pageVO(page, normalizedPageNum, normalizedPageSize);
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
        SysPermission created = findPermissionById(permission.getId());
        operationLogService.recordCreate(MODULE_SYSTEM, BUSINESS_PERMISSION, created.getId(), created.getPermissionCode(), created);
        return created;
    }

    @Override
    @Transactional(transactionManager = MultiDataSourceNames.FA_CLOUD_TRANSACTION_MANAGER)
    public SysPermission updatePermission(Long id, SysPermissionRequestVO request) {
        SystemManagementSupport.requireText(request.permissionCode(), "permissionCode is required");
        SystemManagementSupport.requireText(request.permissionName(), "permissionName is required");
        SysPermission before = findPermissionById(id);
        SysPermission permission = new SysPermission();
        permission.setId(id);
        apply(permission, request);
        if (permissionDao.update(permission) == 0) {
            log.warn("Permission service exception: action=updatePermission, id={}, message=Permission not found", id);
            throw SystemManagementSupport.notFound("Permission not found");
        }
        SysPermission updated = findPermissionById(id);
        operationLogService.recordUpdate(MODULE_SYSTEM, BUSINESS_PERMISSION, id, updated.getPermissionCode(), before, updated);
        return updated;
    }

    @Override
    @Transactional(transactionManager = MultiDataSourceNames.FA_CLOUD_TRANSACTION_MANAGER)
    public void deletePermission(Long id) {
        SysPermission before = findPermissionById(id);
        if (permissionDao.deleteById(id) == 0) {
            log.warn("Permission service exception: action=deletePermission, id={}, message=Permission not found", id);
            throw SystemManagementSupport.notFound("Permission not found");
        }
        operationLogService.recordDelete(MODULE_SYSTEM, BUSINESS_PERMISSION, id, before.getPermissionCode(), before);
    }

    private void apply(SysPermission permission, SysPermissionRequestVO request) {
        permission.setPermissionCode(request.permissionCode());
        permission.setPermissionName(request.permissionName());
        permission.setDescription(request.description());
        permission.setStatus(SystemManagementSupport.defaultStatus(request.status()));
    }
}
