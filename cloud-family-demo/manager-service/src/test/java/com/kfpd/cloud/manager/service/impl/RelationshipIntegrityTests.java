package com.kfpd.cloud.manager.service.impl;

import java.util.List;
import java.util.Map;
import com.kfpd.cloud.manager.dao.cloud.*;
import com.kfpd.cloud.manager.pojo.entity.*;
import com.kfpd.cloud.manager.pojo.vo.*;
import com.kfpd.cloud.manager.service.OperationLogService;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;

class RelationshipIntegrityTests {
    private final SysUserDao users=mock(SysUserDao.class);
    private final SysRoleDao roles=mock(SysRoleDao.class);
    private final SysMenuDao menus=mock(SysMenuDao.class);
    private final SysPermissionDao permissions=mock(SysPermissionDao.class);
    private final OperationLogService logs=mock(OperationLogService.class);
    private final SysPermissionServiceImpl permissionService=new SysPermissionServiceImpl(permissions,logs);
    private final SysRoleServiceImpl roleService=new SysRoleServiceImpl(roles,menus,permissionService,logs);
    private final SysUserServiceImpl userService=new SysUserServiceImpl(users,roleService,logs);
    private SysMenu menu(long id,Long parent) { var m=new SysMenu();m.setId(id);m.setParentId(parent);m.setMenuCode("TEST_"+id);return m; }
    private SysMenuRequestVO menuRequest(Long parent) { return new SysMenuRequestVO(parent,"TEST","测试",null,null,null,false,0,true,"ENABLED"); }
    @Test void userDeletionExplicitlyCleansRoleLinks() {
        var user=new SysUser();user.setId(1L);user.setUsername("test");when(users.findById(1L)).thenReturn(user);when(users.deleteById(1L)).thenReturn(1);
        userService.deleteUser(1L);
        var order=inOrder(users);order.verify(users).deleteUserRoles(1L);order.verify(users).deleteById(1L);
    }
    @Test void roleDeletionExplicitlyCleansAllThreeLinkTables() {
        var role=new SysRole();role.setId(2L);when(roles.findById(2L)).thenReturn(role);when(roles.deleteById(2L)).thenReturn(1);
        roleService.deleteRole(2L);
        var order=inOrder(roles);order.verify(roles).deleteUserRolesByRole(2L);order.verify(roles).deleteRolePermissions(2L);order.verify(roles).deleteRoleMenus(2L);order.verify(roles).deleteById(2L);
    }
    @Test void permissionDeletionCleansMenuAndRoleLinks() {
        var permission=new SysPermission();permission.setId(3L);when(permissions.findById(3L)).thenReturn(permission);when(permissions.deleteById(3L)).thenReturn(1);
        permissionService.deletePermission(3L);
        var order=inOrder(permissions);order.verify(permissions).deleteRolePermissionsByPermission(3L);order.verify(permissions).deleteMenuPermissionsByPermission(3L);order.verify(permissions).deleteById(3L);
    }
    @Test void parentMenuDeletionIsRejectedBeforeAnyCleanup() {
        when(menus.findById(4L)).thenReturn(menu(4,null));when(menus.countChildren(4L)).thenReturn(1L);
        assertThatThrownBy(()->roleService.deleteMenu(4L)).hasMessageContaining("子菜单");
        verify(menus,never()).deleteMenuPermissions(anyLong());verify(menus,never()).deleteById(anyLong());
    }
    @Test void leafMenuDeletionCleansBothAssociations() {
        when(menus.findById(4L)).thenReturn(menu(4,null));when(menus.deleteById(4L)).thenReturn(1);
        roleService.deleteMenu(4L);
        var order=inOrder(menus);order.verify(menus).countChildren(4L);order.verify(menus).deleteMenuPermissions(4L);order.verify(menus).deleteRoleMenusByMenu(4L);order.verify(menus).deleteById(4L);
    }
    @Test void missingParentAndDescendantParentAreRejected() {
        assertThatThrownBy(()->roleService.createMenu(menuRequest(99L))).hasMessageContaining("not found");
        when(menus.findById(4L)).thenReturn(menu(4,null));when(menus.findById(5L)).thenReturn(menu(5,4L));
        assertThatThrownBy(()->roleService.updateMenu(4L,menuRequest(5L))).hasMessageContaining("子菜单");
        verify(menus,never()).insert(any(SysMenu.class));verify(menus,never()).update(any(SysMenu.class));
    }
    @Test void missingAssignmentTargetsAreRejectedBeforeReplacingLinks() {
        var user=new SysUser();user.setId(1L);when(users.findById(1L)).thenReturn(user);
        assertThatThrownBy(()->userService.replaceUserRoles(1L,new IdListVO(List.of(99L)))).hasMessageContaining("not found");
        var role=new SysRole();role.setId(2L);when(roles.findById(2L)).thenReturn(role);
        assertThatThrownBy(()->roleService.replaceRolePermissions(2L,new IdListVO(List.of(99L)))).hasMessageContaining("not found");
        assertThatThrownBy(()->roleService.replaceRoleMenus(2L,new IdListVO(List.of(99L)))).hasMessageContaining("not found");
        when(menus.findById(4L)).thenReturn(menu(4,null));
        assertThatThrownBy(()->roleService.replaceMenuPermissions(4L,new IdListVO(List.of(99L)))).hasMessageContaining("not found");
        verify(users,never()).deleteUserRoles(anyLong());verify(roles,never()).deleteRolePermissions(anyLong());verify(roles,never()).deleteRoleMenus(anyLong());verify(menus,never()).deleteMenuPermissions(anyLong());
    }
    @Test void shopDeletionCleansTypesOnlyWhenNoProductsRemain() {
        var dao=mock(UserFeatureAdminDao.class);var types=mock(CommerceTypeDao.class);
        var service=new UserFeatureAdminServiceImpl(dao,logs,types,mock(MerchantTypeAdminService.class));
        when(types.lockShop(7L)).thenReturn(Map.of("id",7L));when(dao.countCommerceShopProducts(7L)).thenReturn(1L);
        assertThatThrownBy(()->service.delete("commerce-shops",7L)).hasMessageContaining("全部商品");
        verify(types,never()).clearShopTypes(anyLong());
        when(dao.countCommerceShopProducts(7L)).thenReturn(0L);when(dao.deleteCommerceShop(7L)).thenReturn(1);
        service.delete("commerce-shops",7L);
        var order=inOrder(types,dao);order.verify(types).clearShopTypes(7L);order.verify(dao).deleteCommerceShop(7L);
    }
    @Test void merchantTypeIsLockedBeforeReferenceCheckAndDeletion() {
        var dao=mock(DictionaryDao.class);var service=new DictionaryService(dao,logs);
        when(dao.find("items",9L)).thenReturn(Map.of("typeCode","FOOD","itemCode","TEST"));
        when(dao.lockType("FOOD")).thenReturn(Map.of("parentTypeCode","ECOMMERCE"));
        service.delete("items",9L);
        var order=inOrder(dao);order.verify(dao).lockMerchantType("TEST");order.verify(dao).merchantReferences("TEST");order.verify(dao).deleteMerchantType("TEST");order.verify(dao).delete("items",9L);
    }
}
