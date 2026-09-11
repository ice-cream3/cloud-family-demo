package com.kfpd.cloud.manager.service;

import java.util.List;

import com.kfpd.cloud.manager.pojo.vo.IdListVO;
import com.kfpd.cloud.manager.pojo.vo.PageQueryVO;
import com.kfpd.cloud.manager.pojo.vo.PageVO;
import com.kfpd.cloud.manager.pojo.vo.SysMenuRequestVO;
import com.kfpd.cloud.manager.pojo.vo.SysRoleRequestVO;
import com.kfpd.cloud.manager.pojo.entity.SysMenu;
import com.kfpd.cloud.manager.pojo.entity.SysPermission;
import com.kfpd.cloud.manager.pojo.entity.SysRole;

public interface SysRoleService {

    List<SysRole> findRoles();

    PageVO<SysRole> findRoles(PageQueryVO query);

    SysRole findRoleById(Long id);

    SysRole createRole(SysRoleRequestVO request);

    SysRole updateRole(Long id, SysRoleRequestVO request);

    void deleteRole(Long id);

    List<SysPermission> findRolePermissions(Long id);

    List<SysPermission> replaceRolePermissions(Long id, IdListVO request);

    List<SysMenu> findMenus();

    PageVO<SysMenu> findMenus(PageQueryVO query);

    SysMenu findMenuById(Long id);

    SysMenu createMenu(SysMenuRequestVO request);

    SysMenu updateMenu(Long id, SysMenuRequestVO request);

    void deleteMenu(Long id);

    List<SysMenu> findRoleMenus(Long id);

    List<SysMenu> replaceRoleMenus(Long id, IdListVO request);
}
