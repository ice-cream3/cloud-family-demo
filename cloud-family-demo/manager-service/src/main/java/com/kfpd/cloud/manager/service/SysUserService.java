package com.kfpd.cloud.manager.service;

import java.util.List;

import com.kfpd.cloud.manager.pojo.vo.IdListVO;
import com.kfpd.cloud.manager.pojo.dto.LoginAccountDTO;
import com.kfpd.cloud.manager.pojo.dto.SysUserAccessDTO;
import com.kfpd.cloud.manager.pojo.vo.PageQueryVO;
import com.kfpd.cloud.manager.pojo.vo.PageVO;
import com.kfpd.cloud.manager.pojo.vo.SysUserRequestVO;
import com.kfpd.cloud.manager.pojo.entity.SysRole;
import com.kfpd.cloud.manager.pojo.entity.SysUser;

public interface SysUserService {

    List<SysUser> findUsers();

    PageVO<SysUser> findUsers(PageQueryVO query);

    SysUser findUserById(Long id);

    SysUserAccessDTO findUserAccess(Long id);

    LoginAccountDTO findLoginAccount(String username);

    SysUser createUser(SysUserRequestVO request);

    SysUser updateUser(Long id, SysUserRequestVO request);

    void deleteUser(Long id);

    List<SysRole> findUserRoles(Long id);

    List<SysRole> replaceUserRoles(Long id, IdListVO request);
}
