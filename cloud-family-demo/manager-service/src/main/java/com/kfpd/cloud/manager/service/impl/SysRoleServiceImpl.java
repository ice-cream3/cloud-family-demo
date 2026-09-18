package com.kfpd.cloud.manager.service.impl;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.kfpd.cloud.common.config.datasource.MultiDataSourceNames;
import com.kfpd.cloud.manager.dao.SysMenuDao;
import com.kfpd.cloud.manager.dao.SysRoleDao;
import com.kfpd.cloud.manager.pojo.vo.IdListVO;
import com.kfpd.cloud.manager.pojo.vo.PageQueryVO;
import com.kfpd.cloud.manager.pojo.vo.PageVO;
import com.kfpd.cloud.manager.pojo.vo.SysMenuRequestVO;
import com.kfpd.cloud.manager.pojo.vo.SysMenuTreeVO;
import com.kfpd.cloud.manager.pojo.vo.SysRoleRequestVO;
import com.kfpd.cloud.manager.pojo.entity.SysMenu;
import com.kfpd.cloud.manager.pojo.entity.SysPermission;
import com.kfpd.cloud.manager.pojo.entity.SysRole;
import com.kfpd.cloud.manager.service.OperationLogService;
import com.kfpd.cloud.manager.service.SysPermissionService;
import com.kfpd.cloud.manager.service.SysRoleService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SysRoleServiceImpl implements SysRoleService {

    private static final Logger log = LoggerFactory.getLogger(SysRoleServiceImpl.class);
    private static final String MODULE_SYSTEM = "SYSTEM";
    private static final String BUSINESS_ROLE = "SYS_ROLE";
    private static final String BUSINESS_ROLE_PERMISSION = "SYS_ROLE_PERMISSION";
    private static final String BUSINESS_ROLE_MENU = "SYS_ROLE_MENU";
    private static final String BUSINESS_MENU = "SYS_MENU";

    private final SysRoleDao roleDao;
    private final SysMenuDao menuDao;
    private final SysPermissionService permissionService;
    private final OperationLogService operationLogService;

    public SysRoleServiceImpl(SysRoleDao roleDao,
                              SysMenuDao menuDao,
                              SysPermissionService permissionService,
                              OperationLogService operationLogService) {
        this.roleDao = roleDao;
        this.menuDao = menuDao;
        this.permissionService = permissionService;
        this.operationLogService = operationLogService;
    }

    @Override
    public List<SysRole> findRoles() {
        return roleDao.findAll();
    }

    @Override
    public PageVO<SysRole> findRoles(PageQueryVO query) {
        int normalizedPageNum = SystemManagementSupport.pageNum(query);
        int normalizedPageSize = SystemManagementSupport.pageSize(query);
        Page<SysRole> page = roleDao.selectPage(
                new Page<>(normalizedPageNum, normalizedPageSize),
                Wrappers.lambdaQuery(SysRole.class).orderByDesc(SysRole::getId)
        );
        return SystemManagementSupport.pageVO(page, normalizedPageNum, normalizedPageSize);
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
        SysRole created = findRoleById(role.getId());
        operationLogService.recordCreate(MODULE_SYSTEM, BUSINESS_ROLE, created.getId(), created.getRoleCode(), created);
        return created;
    }

    @Override
    @Transactional(transactionManager = MultiDataSourceNames.FA_CLOUD_TRANSACTION_MANAGER)
    public SysRole updateRole(Long id, SysRoleRequestVO request) {
        SystemManagementSupport.requireText(request.roleCode(), "roleCode is required");
        SystemManagementSupport.requireText(request.roleName(), "roleName is required");
        SysRole before = findRoleById(id);
        SysRole role = new SysRole();
        role.setId(id);
        apply(role, request);
        if (roleDao.update(role) == 0) {
            log.warn("Role service exception: action=updateRole, id={}, message=Role not found", id);
            throw SystemManagementSupport.notFound("Role not found");
        }
        SysRole updated = findRoleById(id);
        operationLogService.recordUpdate(MODULE_SYSTEM, BUSINESS_ROLE, id, updated.getRoleCode(), before, updated);
        return updated;
    }

    @Override
    @Transactional(transactionManager = MultiDataSourceNames.FA_CLOUD_TRANSACTION_MANAGER)
    public void deleteRole(Long id) {
        SysRole before = findRoleById(id);
        if (roleDao.deleteById(id) == 0) {
            log.warn("Role service exception: action=deleteRole, id={}, message=Role not found", id);
            throw SystemManagementSupport.notFound("Role not found");
        }
        operationLogService.recordDelete(MODULE_SYSTEM, BUSINESS_ROLE, id, before.getRoleCode(), before);
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
        List<SysPermission> before = roleDao.findPermissionsByRoleId(id);
        roleDao.deleteRolePermissions(id);
        permissionIds.forEach(permissionId -> roleDao.insertRolePermission(id, permissionId));
        List<SysPermission> updated = roleDao.findPermissionsByRoleId(id);
        operationLogService.recordUpdate(MODULE_SYSTEM, BUSINESS_ROLE_PERMISSION, id, String.valueOf(id), before, updated);
        return updated;
    }

    @Override
    public List<SysMenu> findMenus() {
        return menuDao.findAll();
    }

    @Override
    public List<SysMenuTreeVO> findMenuTree() {
        Map<Long, MutableMenuTreeNode> nodeMap = new LinkedHashMap<>();
        for (SysMenu menu : menuDao.findAll()) {
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

    @Override
    public PageVO<SysMenu> findMenus(PageQueryVO query) {
        int normalizedPageNum = SystemManagementSupport.pageNum(query);
        int normalizedPageSize = SystemManagementSupport.pageSize(query);
        Page<SysMenu> page = menuDao.selectPage(
                new Page<>(normalizedPageNum, normalizedPageSize),
                Wrappers.lambdaQuery(SysMenu.class).orderByDesc(SysMenu::getId)
        );
        return SystemManagementSupport.pageVO(page, normalizedPageNum, normalizedPageSize);
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
        SysMenu created = findMenuById(menu.getId());
        operationLogService.recordCreate(MODULE_SYSTEM, BUSINESS_MENU, created.getId(), created.getMenuCode(), created);
        return created;
    }

    @Override
    @Transactional(transactionManager = MultiDataSourceNames.FA_CLOUD_TRANSACTION_MANAGER)
    public SysMenu updateMenu(Long id, SysMenuRequestVO request) {
        SystemManagementSupport.requireText(request.menuCode(), "menuCode is required");
        SystemManagementSupport.requireText(request.menuName(), "menuName is required");
        SysMenu before = findMenuById(id);
        SysMenu menu = new SysMenu();
        menu.setId(id);
        apply(menu, request);
        if (menuDao.update(menu) == 0) {
            log.warn("Role service exception: action=updateMenu, id={}, message=Menu not found", id);
            throw SystemManagementSupport.notFound("Menu not found");
        }
        SysMenu updated = findMenuById(id);
        operationLogService.recordUpdate(MODULE_SYSTEM, BUSINESS_MENU, id, updated.getMenuCode(), before, updated);
        return updated;
    }

    @Override
    @Transactional(transactionManager = MultiDataSourceNames.FA_CLOUD_TRANSACTION_MANAGER)
    public void deleteMenu(Long id) {
        SysMenu before = findMenuById(id);
        if (menuDao.deleteById(id) == 0) {
            log.warn("Role service exception: action=deleteMenu, id={}, message=Menu not found", id);
            throw SystemManagementSupport.notFound("Menu not found");
        }
        operationLogService.recordDelete(MODULE_SYSTEM, BUSINESS_MENU, id, before.getMenuCode(), before);
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
        List<SysMenu> before = roleDao.findMenusByRoleId(id);
        roleDao.deleteRoleMenus(id);
        menuIds.forEach(menuId -> roleDao.insertRoleMenu(id, menuId));
        List<SysMenu> updated = roleDao.findMenusByRoleId(id);
        operationLogService.recordUpdate(MODULE_SYSTEM, BUSINESS_ROLE_MENU, id, String.valueOf(id), before, updated);
        return updated;
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

    private static final class MutableMenuTreeNode {

        private final SysMenu menu;
        private final List<MutableMenuTreeNode> children = new ArrayList<>();

        private MutableMenuTreeNode(SysMenu menu) {
            this.menu = menu;
        }
    }
}
