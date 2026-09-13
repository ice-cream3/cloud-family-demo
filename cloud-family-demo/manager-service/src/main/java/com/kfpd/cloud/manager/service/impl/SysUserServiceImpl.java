package com.kfpd.cloud.manager.service.impl;

import java.util.List;
import java.util.Optional;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.kfpd.cloud.common.config.datasource.MultiDataSourceNames;
import com.kfpd.cloud.manager.dao.SysUserDao;
import com.kfpd.cloud.manager.pojo.vo.IdListVO;
import com.kfpd.cloud.manager.pojo.dto.LoginAccountDTO;
import com.kfpd.cloud.manager.pojo.dto.SysUserAccessDTO;
import com.kfpd.cloud.manager.pojo.vo.PageQueryVO;
import com.kfpd.cloud.manager.pojo.vo.PageVO;
import com.kfpd.cloud.manager.pojo.vo.SysUserRequestVO;
import com.kfpd.cloud.manager.pojo.entity.SysPermission;
import com.kfpd.cloud.manager.pojo.entity.SysRole;
import com.kfpd.cloud.manager.pojo.entity.SysUser;
import com.kfpd.cloud.manager.service.OperationLogService;
import com.kfpd.cloud.manager.service.SysRoleService;
import com.kfpd.cloud.manager.service.SysUserService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SysUserServiceImpl implements SysUserService {

    private static final Logger log = LoggerFactory.getLogger(SysUserServiceImpl.class);
    private static final String MODULE_SYSTEM = "SYSTEM";
    private static final String BUSINESS_USER = "SYS_USER";
    private static final String BUSINESS_USER_ROLE = "SYS_USER_ROLE";

    private final SysUserDao userDao;
    private final SysRoleService roleService;
    private final OperationLogService operationLogService;

    public SysUserServiceImpl(SysUserDao userDao, SysRoleService roleService, OperationLogService operationLogService) {
        this.userDao = userDao;
        this.roleService = roleService;
        this.operationLogService = operationLogService;
    }

    @Override
    public List<SysUser> findUsers() {
        return userDao.findAll();
    }

    @Override
    public PageVO<SysUser> findUsers(PageQueryVO query) {
        int normalizedPageNum = SystemManagementSupport.pageNum(query);
        int normalizedPageSize = SystemManagementSupport.pageSize(query);
        Page<SysUser> page = userDao.selectPage(
                new Page<>(normalizedPageNum, normalizedPageSize),
                Wrappers.lambdaQuery(SysUser.class).orderByDesc(SysUser::getId)
        );
        return SystemManagementSupport.pageVO(page, normalizedPageNum, normalizedPageSize);
    }

    @Override
    public SysUser findUserById(Long id) {
        return Optional.ofNullable(userDao.findById(id)).orElseThrow(() -> {
            log.warn("User service exception: action=findUserById, id={}, message=User not found", id);
            return SystemManagementSupport.notFound("User not found");
        });
    }

    @Override
    public SysUserAccessDTO findUserAccess(Long id) {
        SysUser user = findUserById(id);
        return new SysUserAccessDTO(
                user,
                userDao.findRolesByUserId(id),
                userDao.findPermissionsByUserId(id),
                userDao.findMenusByUserId(id)
        );
    }

    @Override
    public LoginAccountDTO findLoginAccount(String username) {
        SystemManagementSupport.requireText(username, "username is required");
        SysUser user = Optional.ofNullable(userDao.findByUsername(username)).orElseThrow(() -> {
            log.warn("User service exception: action=findLoginAccount, username={}, message=User not found", username);
            return SystemManagementSupport.notFound("User not found");
        });
        if (!"ENABLED".equalsIgnoreCase(user.getStatus())) {
            log.warn("User service exception: action=findLoginAccount, username={}, status={}, message=User not found",
                    username, user.getStatus());
            throw SystemManagementSupport.notFound("User not found");
        }
        List<SysRole> roles = userDao.findRolesByUserId(user.getId()).stream()
                .filter(role -> "ENABLED".equalsIgnoreCase(role.getStatus()))
                .toList();
        List<String> permissions = userDao.findPermissionsByUserId(user.getId()).stream()
                .filter(permission -> "ENABLED".equalsIgnoreCase(permission.getStatus()))
                .map(SysPermission::getPermissionCode)
                .toList();
        return new LoginAccountDTO(
                user.getUsername(),
                user.getPasswordHash(),
                accountType(roles),
                roles.stream().map(SysRole::getRoleCode).toList(),
                permissions
        );
    }

    @Override
    @Transactional(transactionManager = MultiDataSourceNames.FA_CLOUD_TRANSACTION_MANAGER)
    public SysUser createUser(SysUserRequestVO request) {
        SystemManagementSupport.requireText(request.username(), "username is required");
        SystemManagementSupport.requireText(request.displayName(), "displayName is required");
        SysUser user = new SysUser();
        apply(user, request);
        userDao.insert(user);
        SysUser created = findUserById(user.getId());
        operationLogService.recordCreate(MODULE_SYSTEM, BUSINESS_USER, created.getId(), created.getUsername(), created);
        return created;
    }

    @Override
    @Transactional(transactionManager = MultiDataSourceNames.FA_CLOUD_TRANSACTION_MANAGER)
    public SysUser updateUser(Long id, SysUserRequestVO request) {
        SystemManagementSupport.requireText(request.username(), "username is required");
        SystemManagementSupport.requireText(request.displayName(), "displayName is required");
        SysUser before = findUserById(id);
        SysUser user = new SysUser();
        user.setId(id);
        apply(user, request);
        if (userDao.update(user) == 0) {
            log.warn("User service exception: action=updateUser, id={}, message=User not found", id);
            throw SystemManagementSupport.notFound("User not found");
        }
        SysUser updated = findUserById(id);
        operationLogService.recordUpdate(MODULE_SYSTEM, BUSINESS_USER, id, updated.getUsername(), before, updated);
        return updated;
    }

    @Override
    @Transactional(transactionManager = MultiDataSourceNames.FA_CLOUD_TRANSACTION_MANAGER)
    public void deleteUser(Long id) {
        SysUser before = findUserById(id);
        if (userDao.deleteById(id) == 0) {
            log.warn("User service exception: action=deleteUser, id={}, message=User not found", id);
            throw SystemManagementSupport.notFound("User not found");
        }
        operationLogService.recordDelete(MODULE_SYSTEM, BUSINESS_USER, id, before.getUsername(), before);
    }

    @Override
    public List<SysRole> findUserRoles(Long id) {
        findUserById(id);
        return userDao.findRolesByUserId(id);
    }

    @Override
    @Transactional(transactionManager = MultiDataSourceNames.FA_CLOUD_TRANSACTION_MANAGER)
    public List<SysRole> replaceUserRoles(Long id, IdListVO request) {
        findUserById(id);
        List<Long> roleIds = SystemManagementSupport.ids(request);
        roleIds.forEach(roleService::findRoleById);
        List<SysRole> before = userDao.findRolesByUserId(id);
        userDao.deleteUserRoles(id);
        roleIds.forEach(roleId -> userDao.insertUserRole(id, roleId));
        List<SysRole> updated = userDao.findRolesByUserId(id);
        operationLogService.recordUpdate(MODULE_SYSTEM, BUSINESS_USER_ROLE, id, String.valueOf(id), before, updated);
        return updated;
    }

    private void apply(SysUser user, SysUserRequestVO request) {
        user.setUsername(request.username());
        user.setPasswordHash(request.passwordHash());
        user.setDisplayName(request.displayName());
        user.setEmail(request.email());
        user.setStatus(SystemManagementSupport.defaultStatus(request.status()));
    }

    private String accountType(List<SysRole> roles) {
        return roles.stream().anyMatch(role -> "MANAGER".equalsIgnoreCase(role.getRoleCode())) ? "MANAGER" : "API";
    }
}
