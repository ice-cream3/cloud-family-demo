package com.kfpd.cloud.manager.service.impl;

import java.util.List;
import java.util.Optional;

import com.kfpd.cloud.common.datasource.MultiDataSourceNames;
import com.kfpd.cloud.manager.dao.SysMenuDao;
import com.kfpd.cloud.manager.dao.SysRoleDao;
import com.kfpd.cloud.manager.pojo.vo.IdListVO;
import com.kfpd.cloud.manager.pojo.vo.SysMenuRequestVO;
import com.kfpd.cloud.manager.pojo.vo.SysRoleRequestVO;
import com.kfpd.cloud.manager.pojo.entity.SysMenu;
import com.kfpd.cloud.manager.pojo.entity.SysPermission;
import com.kfpd.cloud.manager.pojo.entity.SysRole;
import com.kfpd.cloud.manager.service.SysPermissionService;
import com.kfpd.cloud.manager.service.SysRoleService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SysRoleServiceImpl implements SysRoleService {

    private static final Logger log = LoggerFactory.getLogger(SysRoleServiceImpl.class);

    private final SysRoleDao roleDao;
    private final SysMenuDao menuDao;
    private final SysPermissionService permissionService;

    public SysRoleServiceImpl(SysRoleDao roleDao, SysMenuDao menuDao, SysPermissionService permissionService) {
        this.roleDao = roleDao;
        this.menuDao = menuDao;
        this.permissionService = permissionService;
    }

    @Override
    public List<SysRole> findRoles() {
        return roleDao.findAll();
    }

    @Override
    public SysRole findRoleById(Long id) {
        return Optional.ofNullable(roleDao.findById(id)).orElseThrow(() -> {
            log.warn("Role service exception: action=findRoleById, id={}, message=Role not found", id);
            return SystemManagementSupport.notFound("Role not found");
        });
    }

    @Override
    @Transactional(transactionManager = MultiDataSourceNames.FA_CLOUD_TRANSACTION_MANAGER)
    public SysRole createRole(SysRoleRequestVO request) {
        SystemManagementSupport.requireText(request.roleCode(), "roleCode is required");
        SystemManagementSupport.requireText(request.roleName(), "roleName is required");
        SysRole role = new SysRole();
        apply(role, request);
        roleDao.insert(role);
        return findRoleById(role.getId());
    }

    @Override
    @Transactional(transactionManager = MultiDataSourceNames.FA_CLOUD_TRANSACTION_MANAGER)
    public SysRole updateRole(Long id, SysRoleRequestVO request) {
        SystemManagementSupport.requireText(request.roleCode(), "roleCode is required");
        SystemManagementSupport.requireText(request.roleName(), "roleName is required");
        SysRole role = new SysRole();
        role.setId(id);
        apply(role, request);
        if (roleDao.update(role) == 0) {
            log.warn("Role service exception: action=updateRole, id={}, message=Role not found", id);
            throw SystemManagementSupport.notFound("Role not found");
        }
        return findRoleById(id);
    }

    @Override
    @Transactional(transactionManager = MultiDataSourceNames.FA_CLOUD_TRANSACTION_MANAGER)
    public void deleteRole(Long id) {
        if (roleDao.deleteById(id) == 0) {
            log.warn("Role service exception: action=deleteRole, id={}, message=Role not found", id);
            throw SystemManagementSupport.notFound("Role not found");
        }
    }

    @Override
    public List<SysPermission> findRolePermissions(Long id) {
        findRoleById(id);
        return roleDao.findPermissionsByRoleId(id);
    }

    @Override
    @Transactional(transactionManager = MultiDataSourceNames.FA_CLOUD_TRANSACTION_MANAGER)
    public List<SysPermission> replaceRolePermissions(Long id, IdListVO request) {
        findRoleById(id);
        List<Long> permissionIds = SystemManagementSupport.ids(request);
        permissionIds.forEach(permissionService::findPermissionById);
        roleDao.deleteRolePermissions(id);
        permissionIds.forEach(permissionId -> roleDao.insertRolePermission(id, permissionId));
        return roleDao.findPermissionsByRoleId(id);
    }

    @Override
    public List<SysMenu> findMenus() {
        return menuDao.findAll();
    }

    @Override
    public SysMenu findMenuById(Long id) {
        return Optional.ofNullable(menuDao.findById(id)).orElseThrow(() -> {
            log.warn("Role service exception: action=findMenuById, id={}, message=Menu not found", id);
            return SystemManagementSupport.notFound("Menu not found");
        });
    }

    @Override
    @Transactional(transactionManager = MultiDataSourceNames.FA_CLOUD_TRANSACTION_MANAGER)
    public SysMenu createMenu(SysMenuRequestVO request) {
        SystemManagementSupport.requireText(request.menuCode(), "menuCode is required");
        SystemManagementSupport.requireText(request.menuName(), "menuName is required");
        SysMenu menu = new SysMenu();
        apply(menu, request);
        menuDao.insert(menu);
        return findMenuById(menu.getId());
    }

    @Override
    @Transactional(transactionManager = MultiDataSourceNames.FA_CLOUD_TRANSACTION_MANAGER)
    public SysMenu updateMenu(Long id, SysMenuRequestVO request) {
        SystemManagementSupport.requireText(request.menuCode(), "menuCode is required");
        SystemManagementSupport.requireText(request.menuName(), "menuName is required");
        SysMenu menu = new SysMenu();
        menu.setId(id);
        apply(menu, request);
        if (menuDao.update(menu) == 0) {
            log.warn("Role service exception: action=updateMenu, id={}, message=Menu not found", id);
            throw SystemManagementSupport.notFound("Menu not found");
        }
        return findMenuById(id);
    }

    @Override
    @Transactional(transactionManager = MultiDataSourceNames.FA_CLOUD_TRANSACTION_MANAGER)
    public void deleteMenu(Long id) {
        if (menuDao.deleteById(id) == 0) {
            log.warn("Role service exception: action=deleteMenu, id={}, message=Menu not found", id);
            throw SystemManagementSupport.notFound("Menu not found");
        }
    }

    @Override
    public List<SysMenu> findRoleMenus(Long id) {
        findRoleById(id);
        return roleDao.findMenusByRoleId(id);
    }

    @Override
    @Transactional(transactionManager = MultiDataSourceNames.FA_CLOUD_TRANSACTION_MANAGER)
    public List<SysMenu> replaceRoleMenus(Long id, IdListVO request) {
        findRoleById(id);
        List<Long> menuIds = SystemManagementSupport.ids(request);
        menuIds.forEach(this::findMenuById);
        roleDao.deleteRoleMenus(id);
        menuIds.forEach(menuId -> roleDao.insertRoleMenu(id, menuId));
        return roleDao.findMenusByRoleId(id);
    }

    private void apply(SysRole role, SysRoleRequestVO request) {
        role.setRoleCode(request.roleCode());
        role.setRoleName(request.roleName());
        role.setDescription(request.description());
        role.setStatus(SystemManagementSupport.defaultStatus(request.status()));
    }

    private void apply(SysMenu menu, SysMenuRequestVO request) {
        menu.setParentId(request.parentId());
        menu.setMenuCode(request.menuCode());
        menu.setMenuName(request.menuName());
        menu.setPath(request.path());
        menu.setComponent(request.component());
        menu.setIcon(request.icon());
        menu.setSortOrder(request.sortOrder() == null ? 0 : request.sortOrder());
        menu.setVisible(request.visible() == null ? Boolean.TRUE : request.visible());
        menu.setStatus(SystemManagementSupport.defaultStatus(request.status()));
    }
}
