package com.kfpd.cloud.manager.service;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import com.kfpd.cloud.common.datasource.MultiDataSourceNames;
import com.kfpd.cloud.manager.dao.SysMenuDao;
import com.kfpd.cloud.manager.dao.SysPermissionDao;
import com.kfpd.cloud.manager.dao.SysRoleDao;
import com.kfpd.cloud.manager.dao.SysUserDao;
import com.kfpd.cloud.manager.pojo.dto.IdListDTO;
import com.kfpd.cloud.manager.pojo.dto.LoginAccountDTO;
import com.kfpd.cloud.manager.pojo.dto.SysMenuRequestDTO;
import com.kfpd.cloud.manager.pojo.dto.SysPermissionRequestDTO;
import com.kfpd.cloud.manager.pojo.dto.SysRoleRequestDTO;
import com.kfpd.cloud.manager.pojo.dto.SysUserAccessDTO;
import com.kfpd.cloud.manager.pojo.dto.SysUserRequestDTO;
import com.kfpd.cloud.manager.pojo.entity.SysMenu;
import com.kfpd.cloud.manager.pojo.entity.SysPermission;
import com.kfpd.cloud.manager.pojo.entity.SysRole;
import com.kfpd.cloud.manager.pojo.entity.SysUser;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.NOT_FOUND;

@Service
public class SystemManagementService {

    private final SysUserDao userDao;
    private final SysRoleDao roleDao;
    private final SysPermissionDao permissionDao;
    private final SysMenuDao menuDao;

    public SystemManagementService(SysUserDao userDao,
                                   SysRoleDao roleDao,
                                   SysPermissionDao permissionDao,
                                   SysMenuDao menuDao) {
        this.userDao = userDao;
        this.roleDao = roleDao;
        this.permissionDao = permissionDao;
        this.menuDao = menuDao;
    }

    public List<SysUser> findUsers() {
        return userDao.findAll();
    }

    public SysUser findUserById(Long id) {
        return Optional.ofNullable(userDao.findById(id)).orElseThrow(() -> notFound("User not found"));
    }

    public SysUserAccessDTO findUserAccess(Long id) {
        SysUser user = findUserById(id);
        return new SysUserAccessDTO(
                user,
                userDao.findRolesByUserId(id),
                userDao.findPermissionsByUserId(id),
                userDao.findMenusByUserId(id)
        );
    }

    public LoginAccountDTO findLoginAccount(String username) {
        requireText(username, "username is required");
        SysUser user = Optional.ofNullable(userDao.findByUsername(username)).orElseThrow(() -> notFound("User not found"));
        if (!"ENABLED".equalsIgnoreCase(user.getStatus())) {
            throw notFound("User not found");
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

    @Transactional(transactionManager = MultiDataSourceNames.FA_CLOUD_TRANSACTION_MANAGER)
    public SysUser createUser(SysUserRequestDTO request) {
        requireText(request.username(), "username is required");
        requireText(request.displayName(), "displayName is required");
        SysUser user = new SysUser();
        apply(user, request);
        userDao.insert(user);
        return findUserById(user.getId());
    }

    @Transactional(transactionManager = MultiDataSourceNames.FA_CLOUD_TRANSACTION_MANAGER)
    public SysUser updateUser(Long id, SysUserRequestDTO request) {
        requireText(request.username(), "username is required");
        requireText(request.displayName(), "displayName is required");
        SysUser user = new SysUser();
        user.setId(id);
        apply(user, request);
        if (userDao.update(user) == 0) {
            throw notFound("User not found");
        }
        return findUserById(id);
    }

    @Transactional(transactionManager = MultiDataSourceNames.FA_CLOUD_TRANSACTION_MANAGER)
    public void deleteUser(Long id) {
        if (userDao.deleteById(id) == 0) {
            throw notFound("User not found");
        }
    }

    public List<SysRole> findUserRoles(Long id) {
        findUserById(id);
        return userDao.findRolesByUserId(id);
    }

    @Transactional(transactionManager = MultiDataSourceNames.FA_CLOUD_TRANSACTION_MANAGER)
    public List<SysRole> replaceUserRoles(Long id, IdListDTO request) {
        findUserById(id);
        List<Long> roleIds = ids(request);
        roleIds.forEach(this::findRoleById);
        userDao.deleteUserRoles(id);
        roleIds.forEach(roleId -> userDao.insertUserRole(id, roleId));
        return userDao.findRolesByUserId(id);
    }

    public List<SysRole> findRoles() {
        return roleDao.findAll();
    }

    public SysRole findRoleById(Long id) {
        return Optional.ofNullable(roleDao.findById(id)).orElseThrow(() -> notFound("Role not found"));
    }

    @Transactional(transactionManager = MultiDataSourceNames.FA_CLOUD_TRANSACTION_MANAGER)
    public SysRole createRole(SysRoleRequestDTO request) {
        requireText(request.roleCode(), "roleCode is required");
        requireText(request.roleName(), "roleName is required");
        SysRole role = new SysRole();
        apply(role, request);
        roleDao.insert(role);
        return findRoleById(role.getId());
    }

    @Transactional(transactionManager = MultiDataSourceNames.FA_CLOUD_TRANSACTION_MANAGER)
    public SysRole updateRole(Long id, SysRoleRequestDTO request) {
        requireText(request.roleCode(), "roleCode is required");
        requireText(request.roleName(), "roleName is required");
        SysRole role = new SysRole();
        role.setId(id);
        apply(role, request);
        if (roleDao.update(role) == 0) {
            throw notFound("Role not found");
        }
        return findRoleById(id);
    }

    @Transactional(transactionManager = MultiDataSourceNames.FA_CLOUD_TRANSACTION_MANAGER)
    public void deleteRole(Long id) {
        if (roleDao.deleteById(id) == 0) {
            throw notFound("Role not found");
        }
    }

    public List<SysPermission> findRolePermissions(Long id) {
        findRoleById(id);
        return roleDao.findPermissionsByRoleId(id);
    }

    @Transactional(transactionManager = MultiDataSourceNames.FA_CLOUD_TRANSACTION_MANAGER)
    public List<SysPermission> replaceRolePermissions(Long id, IdListDTO request) {
        findRoleById(id);
        List<Long> permissionIds = ids(request);
        permissionIds.forEach(this::findPermissionById);
        roleDao.deleteRolePermissions(id);
        permissionIds.forEach(permissionId -> roleDao.insertRolePermission(id, permissionId));
        return roleDao.findPermissionsByRoleId(id);
    }

    public List<SysPermission> findPermissions() {
        return permissionDao.findAll();
    }

    public SysPermission findPermissionById(Long id) {
        return Optional.ofNullable(permissionDao.findById(id)).orElseThrow(() -> notFound("Permission not found"));
    }

    @Transactional(transactionManager = MultiDataSourceNames.FA_CLOUD_TRANSACTION_MANAGER)
    public SysPermission createPermission(SysPermissionRequestDTO request) {
        requireText(request.permissionCode(), "permissionCode is required");
        requireText(request.permissionName(), "permissionName is required");
        SysPermission permission = new SysPermission();
        apply(permission, request);
        permissionDao.insert(permission);
        return findPermissionById(permission.getId());
    }

    @Transactional(transactionManager = MultiDataSourceNames.FA_CLOUD_TRANSACTION_MANAGER)
    public SysPermission updatePermission(Long id, SysPermissionRequestDTO request) {
        requireText(request.permissionCode(), "permissionCode is required");
        requireText(request.permissionName(), "permissionName is required");
        SysPermission permission = new SysPermission();
        permission.setId(id);
        apply(permission, request);
        if (permissionDao.update(permission) == 0) {
            throw notFound("Permission not found");
        }
        return findPermissionById(id);
    }

    @Transactional(transactionManager = MultiDataSourceNames.FA_CLOUD_TRANSACTION_MANAGER)
    public void deletePermission(Long id) {
        if (permissionDao.deleteById(id) == 0) {
            throw notFound("Permission not found");
        }
    }

    public List<SysMenu> findMenus() {
        return menuDao.findAll();
    }

    public SysMenu findMenuById(Long id) {
        return Optional.ofNullable(menuDao.findById(id)).orElseThrow(() -> notFound("Menu not found"));
    }

    @Transactional(transactionManager = MultiDataSourceNames.FA_CLOUD_TRANSACTION_MANAGER)
    public SysMenu createMenu(SysMenuRequestDTO request) {
        requireText(request.menuCode(), "menuCode is required");
        requireText(request.menuName(), "menuName is required");
        SysMenu menu = new SysMenu();
        apply(menu, request);
        menuDao.insert(menu);
        return findMenuById(menu.getId());
    }

    @Transactional(transactionManager = MultiDataSourceNames.FA_CLOUD_TRANSACTION_MANAGER)
    public SysMenu updateMenu(Long id, SysMenuRequestDTO request) {
        requireText(request.menuCode(), "menuCode is required");
        requireText(request.menuName(), "menuName is required");
        SysMenu menu = new SysMenu();
        menu.setId(id);
        apply(menu, request);
        if (menuDao.update(menu) == 0) {
            throw notFound("Menu not found");
        }
        return findMenuById(id);
    }

    @Transactional(transactionManager = MultiDataSourceNames.FA_CLOUD_TRANSACTION_MANAGER)
    public void deleteMenu(Long id) {
        if (menuDao.deleteById(id) == 0) {
            throw notFound("Menu not found");
        }
    }

    public List<SysMenu> findRoleMenus(Long id) {
        findRoleById(id);
        return roleDao.findMenusByRoleId(id);
    }

    @Transactional(transactionManager = MultiDataSourceNames.FA_CLOUD_TRANSACTION_MANAGER)
    public List<SysMenu> replaceRoleMenus(Long id, IdListDTO request) {
        findRoleById(id);
        List<Long> menuIds = ids(request);
        menuIds.forEach(this::findMenuById);
        roleDao.deleteRoleMenus(id);
        menuIds.forEach(menuId -> roleDao.insertRoleMenu(id, menuId));
        return roleDao.findMenusByRoleId(id);
    }

    private void apply(SysUser user, SysUserRequestDTO request) {
        user.setUsername(request.username());
        user.setPasswordHash(request.passwordHash());
        user.setDisplayName(request.displayName());
        user.setEmail(request.email());
        user.setStatus(defaultStatus(request.status()));
    }

    private void apply(SysRole role, SysRoleRequestDTO request) {
        role.setRoleCode(request.roleCode());
        role.setRoleName(request.roleName());
        role.setDescription(request.description());
        role.setStatus(defaultStatus(request.status()));
    }

    private void apply(SysPermission permission, SysPermissionRequestDTO request) {
        permission.setPermissionCode(request.permissionCode());
        permission.setPermissionName(request.permissionName());
        permission.setDescription(request.description());
        permission.setStatus(defaultStatus(request.status()));
    }

    private void apply(SysMenu menu, SysMenuRequestDTO request) {
        menu.setParentId(request.parentId());
        menu.setMenuCode(request.menuCode());
        menu.setMenuName(request.menuName());
        menu.setPath(request.path());
        menu.setComponent(request.component());
        menu.setIcon(request.icon());
        menu.setSortOrder(request.sortOrder() == null ? 0 : request.sortOrder());
        menu.setVisible(request.visible() == null ? Boolean.TRUE : request.visible());
        menu.setStatus(defaultStatus(request.status()));
    }

    private ResponseStatusException notFound(String message) {
        return new ResponseStatusException(NOT_FOUND, message);
    }

    private void requireText(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new ResponseStatusException(BAD_REQUEST, message);
        }
    }

    private List<Long> ids(IdListDTO request) {
        if (request == null || request.ids() == null) {
            return List.of();
        }
        Set<Long> distinct = new LinkedHashSet<>();
        for (Long id : request.ids()) {
            if (id != null) {
                distinct.add(id);
            }
        }
        return new ArrayList<>(distinct);
    }

    private String defaultStatus(String status) {
        return status == null || status.isBlank() ? "ENABLED" : status;
    }

    private String accountType(List<SysRole> roles) {
        return roles.stream().anyMatch(role -> "MANAGER".equalsIgnoreCase(role.getRoleCode())) ? "MANAGER" : "API";
    }
}
