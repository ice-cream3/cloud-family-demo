package com.kfpd.cloud.manager.service;

import java.util.List;

import com.kfpd.cloud.manager.pojo.vo.IdListVO;
import com.kfpd.cloud.manager.pojo.dto.LoginAccountDTO;
import com.kfpd.cloud.manager.pojo.dto.SysUserAccessDTO;
import com.kfpd.cloud.manager.pojo.vo.PageQueryVO;
import com.kfpd.cloud.manager.pojo.vo.PageVO;
import com.kfpd.cloud.manager.pojo.vo.PasswordResetRequestVO;
import com.kfpd.cloud.manager.pojo.vo.SysMenuTreeVO;
import com.kfpd.cloud.manager.pojo.vo.SysUserRequestVO;
import com.kfpd.cloud.manager.pojo.vo.SysUserVO;
import com.kfpd.cloud.manager.pojo.entity.SysRole;

public interface SysUserService {

    List<SysUserVO> findUsers();

    PageVO<SysUserVO> findUsers(PageQueryVO query);

    SysUserVO findUserById(Long id);

    SysUserAccessDTO findUserAccess(Long id);

    List<SysMenuTreeVO> findUserMenuTree(String username);

    LoginAccountDTO findLoginAccount(String username);

    SysUserVO createUser(SysUserRequestVO request);

    SysUserVO updateUser(Long id, SysUserRequestVO request);

    SysUserVO resetUserPassword(Long id, PasswordResetRequestVO request);

    void deleteUser(Long id);

    List<SysRole> findUserRoles(Long id);

    List<SysRole> replaceUserRoles(Long id, IdListVO request);
}
