package com.kfpd.cloud.manager.service.impl;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.kfpd.cloud.common.config.datasource.MultiDataSourceNames;
import com.kfpd.cloud.manager.dao.SysUserDao;
import com.kfpd.cloud.manager.pojo.vo.IdListVO;
import com.kfpd.cloud.manager.pojo.dto.LoginAccountDTO;
import com.kfpd.cloud.manager.pojo.dto.SysUserAccessDTO;
import com.kfpd.cloud.manager.pojo.vo.PageQueryVO;
import com.kfpd.cloud.manager.pojo.vo.PageVO;
import com.kfpd.cloud.manager.pojo.vo.PasswordResetRequestVO;
import com.kfpd.cloud.manager.pojo.vo.SysMenuTreeVO;
import com.kfpd.cloud.manager.pojo.vo.SysUserRequestVO;
import com.kfpd.cloud.manager.pojo.vo.SysUserVO;
import com.kfpd.cloud.manager.pojo.entity.SysMenu;
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
    public List<SysUserVO> findUsers() {
        return userDao.findAll().stream().map(this::toVO).toList();
    }

    @Override
    public PageVO<SysUserVO> findUsers(PageQueryVO query) {
        int normalizedPageNum = SystemManagementSupport.pageNum(query);
        int normalizedPageSize = SystemManagementSupport.pageSize(query);
        String keyword = SystemManagementSupport.keyword(query);
        String status = SystemManagementSupport.status(query);
        LambdaQueryWrapper<SysUser> wrapper = Wrappers.lambdaQuery(SysUser.class)
                .eq(SystemManagementSupport.hasText(status), SysUser::getStatus, status)
                .and(SystemManagementSupport.hasText(keyword), condition -> condition
                        .like(SysUser::getUsername, keyword)
                        .or()
                        .like(SysUser::getDisplayName, keyword)
                        .or()
                        .like(SysUser::getEmail, keyword)
                )
                .orderByDesc(SysUser::getId);
        Page<SysUser> page = userDao.selectPage(
                new Page<>(normalizedPageNum, normalizedPageSize),
                wrapper
        );
        return new PageVO<>(
                page.getTotal(),
                normalizedPageNum,
                normalizedPageSize,
                page.getRecords().stream().map(this::toVO).toList()
        );
    }

    @Override
    public SysUserVO findUserById(Long id) {
        return toVO(findUserEntityById(id));
    }

    private SysUser findUserEntityById(Long id) {
        return Optional.ofNullable(userDao.findById(id)).orElseThrow(() -> {
            log.warn("User service exception: action=findUserById, id={}, message=User not found", id);
            return SystemManagementSupport.notFound("User not found");
        });
    }

    @Override
    public SysUserAccessDTO findUserAccess(Long id) {
        SysUser user = findUserEntityById(id);
        return new SysUserAccessDTO(
                toVO(user),
                userDao.findRolesByUserId(id),
                userDao.findPermissionsByUserId(id),
                userDao.findMenusByUserId(id)
        );
    }

    @Override
    public List<SysMenuTreeVO> findUserMenuTree(String username) {
        SystemManagementSupport.requireText(username, "username is required");
        SysUser user = Optional.ofNullable(userDao.findByUsername(username)).orElseThrow(() -> {
            log.warn("User service exception: action=findUserMenuTree, username={}, message=User not found", username);
            return SystemManagementSupport.notFound("User not found");
        });
        return buildMenuTree(
                userDao.findMenusByUserId(user.getId()).stream()
                        .filter(menu -> Boolean.TRUE.equals(menu.getVisible()))
                        .filter(menu -> "ENABLED".equalsIgnoreCase(menu.getStatus()))
                        .toList()
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
    public SysUserVO createUser(SysUserRequestVO request) {
        requireRequest(request);
        SystemManagementSupport.requireText(request.username(), "username is required");
        SystemManagementSupport.requireText(request.displayName(), "displayName is required");
        if (userDao.findByUsername(request.username()) != null) {
            log.warn("User service exception: action=createUser, username={}, message=用户已存在", request.username());
            throw SystemManagementSupport.badRequest("用户已存在");
        }
        SysUser user = new SysUser();
        apply(user, request);
        userDao.insert(user);
        SysUser created = findUserEntityById(user.getId());
        SysUserVO createdVO = toVO(created);
        operationLogService.recordCreate(MODULE_SYSTEM, BUSINESS_USER, createdVO.id(), createdVO.username(), createdVO);
        return createdVO;
    }

    @Override
    @Transactional(transactionManager = MultiDataSourceNames.FA_CLOUD_TRANSACTION_MANAGER)
    public SysUserVO updateUser(Long id, SysUserRequestVO request) {
        requireRequest(request);
        SystemManagementSupport.requireText(request.username(), "username is required");
        SystemManagementSupport.requireText(request.displayName(), "displayName is required");
        SysUser before = findUserEntityById(id);
        SysUser user = new SysUser();
        user.setId(id);
        apply(user, request);
        user.setUsername(before.getUsername());
        if (!SystemManagementSupport.hasText(request.resolvedPasswordHash())) {
            user.setPasswordHash(before.getPasswordHash());
        }
        if (userDao.update(user) == 0) {
            log.warn("User service exception: action=updateUser, id={}, message=User not found", id);
            throw SystemManagementSupport.notFound("User not found");
        }
        SysUser updated = findUserEntityById(id);
        SysUserVO updatedVO = toVO(updated);
        operationLogService.recordUpdate(MODULE_SYSTEM, BUSINESS_USER, id, updatedVO.username(), toVO(before), updatedVO);
        return updatedVO;
    }

    @Override
    @Transactional(transactionManager = MultiDataSourceNames.FA_CLOUD_TRANSACTION_MANAGER)
    public SysUserVO resetUserPassword(Long id, PasswordResetRequestVO request) {
        if (request == null) {
            throw SystemManagementSupport.badRequest("request body is required");
        }
        SystemManagementSupport.requireText(request.resolvedPasswordHash(), "password is required");
        SysUser before = findUserEntityById(id);
        SysUser user = new SysUser();
        user.setId(id);
        user.setUsername(before.getUsername());
        user.setPasswordHash(request.resolvedPasswordHash());
        user.setDisplayName(before.getDisplayName());
        user.setEmail(before.getEmail());
        user.setStatus(before.getStatus());
        if (userDao.update(user) == 0) {
            log.warn("User service exception: action=resetUserPassword, id={}, message=User not found", id);
            throw SystemManagementSupport.notFound("User not found");
        }
        SysUser updated = findUserEntityById(id);
        SysUserVO updatedVO = toVO(updated);
        operationLogService.recordUpdate(MODULE_SYSTEM, BUSINESS_USER, id, updatedVO.username(), "PASSWORD_RESET", updatedVO);
        return updatedVO;
    }

    @Override
    @Transactional(transactionManager = MultiDataSourceNames.FA_CLOUD_TRANSACTION_MANAGER)
    public void deleteUser(Long id) {
        SysUser before = findUserEntityById(id);
        if (userDao.deleteById(id) == 0) {
            log.warn("User service exception: action=deleteUser, id={}, message=User not found", id);
            throw SystemManagementSupport.notFound("User not found");
        }
        operationLogService.recordDelete(MODULE_SYSTEM, BUSINESS_USER, id, before.getUsername(), toVO(before));
    }

    @Override
    public List<SysRole> findUserRoles(Long id) {
        findUserEntityById(id);
        return userDao.findRolesByUserId(id);
    }

    @Override
    @Transactional(transactionManager = MultiDataSourceNames.FA_CLOUD_TRANSACTION_MANAGER)
    public List<SysRole> replaceUserRoles(Long id, IdListVO request) {
        findUserEntityById(id);
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
        user.setPasswordHash(request.resolvedPasswordHash());
        user.setDisplayName(request.displayName());
        user.setEmail(request.email());
        user.setStatus(SystemManagementSupport.defaultStatus(request.status()));
    }

    private void requireRequest(SysUserRequestVO request) {
        if (request == null) {
            throw SystemManagementSupport.badRequest("request body is required");
        }
    }

    private SysUserVO toVO(SysUser user) {
        return new SysUserVO(
                user.getId(),
                user.getUsername(),
                user.getDisplayName(),
                user.getEmail(),
                user.getStatus(),
                user.getCreatedAt(),
                user.getUpdatedAt()
        );
    }

    private List<SysMenuTreeVO> buildMenuTree(List<SysMenu> menus) {
        Map<Long, MutableMenuTreeNode> nodeMap = new LinkedHashMap<>();
        for (SysMenu menu : menus) {
            nodeMap.put(menu.getId(), new MutableMenuTreeNode(menu));
        }

        List<MutableMenuTreeNode> roots = new ArrayList<>();
        for (MutableMenuTreeNode node : nodeMap.values()) {
            Long parentId = node.menu.getParentId();
            MutableMenuTreeNode parent = parentId == null ? null : nodeMap.get(parentId);
            if (parent == null) {
                roots.add(node);
            } else {
                parent.children.add(node);
            }
        }
        return roots.stream().map(this::toTreeVO).toList();
    }

    private SysMenuTreeVO toTreeVO(MutableMenuTreeNode node) {
        SysMenu menu = node.menu;
        return new SysMenuTreeVO(
                menu.getId(),
                menu.getParentId(),
                menu.getMenuCode(),
                menu.getMenuName(),
                menu.getPath(),
                menu.getComponent(),
                menu.getIcon(),
                menu.getSortOrder(),
                menu.getVisible(),
                menu.getStatus(),
                menu.getCreatedAt(),
                menu.getUpdatedAt(),
                node.children.stream().map(this::toTreeVO).toList()
        );
    }

    private String accountType(List<SysRole> roles) {
        return roles.stream().anyMatch(role -> "MANAGER".equalsIgnoreCase(role.getRoleCode())) ? "MANAGER" : "API";
    }

    private static final class MutableMenuTreeNode {

        private final SysMenu menu;
        private final List<MutableMenuTreeNode> children = new ArrayList<>();

        private MutableMenuTreeNode(SysMenu menu) {
            this.menu = menu;
        }
    }
}
