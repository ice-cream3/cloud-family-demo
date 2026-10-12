package com.kfpd.cloud.manager.service;

import com.kfpd.cloud.manager.pojo.vo.PageVO;
import com.kfpd.cloud.manager.pojo.vo.PasswordResetRequestVO;
import com.kfpd.cloud.manager.pojo.vo.VipUserPageQueryVO;
import com.kfpd.cloud.manager.pojo.vo.VipUserRequestVO;
import com.kfpd.cloud.manager.pojo.vo.VipUserVO;

public interface VipUserService {

    PageVO<VipUserVO> findVipUsers(VipUserPageQueryVO query);

    PageVO<VipUserVO> findPartnerInfoUsers(VipUserPageQueryVO query, String authorization);

    VipUserVO findVipUserById(Long id);

    VipUserVO createVipUser(VipUserRequestVO request);

    VipUserVO updateVipUser(Long id, VipUserRequestVO request);

    VipUserVO resetVipUserPassword(Long id, PasswordResetRequestVO request);

    void deleteVipUser(Long id);

    int kickOutVipUser(Long id, String authorization);
}
