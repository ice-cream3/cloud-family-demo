package com.kfpd.cloud.manager.service;

import com.kfpd.cloud.manager.pojo.vo.PageVO;
import com.kfpd.cloud.manager.pojo.vo.VipUserPageQueryVO;
import com.kfpd.cloud.manager.pojo.vo.VipUserRequestVO;
import com.kfpd.cloud.manager.pojo.vo.VipUserVO;

public interface VipUserService {

    PageVO<VipUserVO> findVipUsers(VipUserPageQueryVO query);

    VipUserVO findVipUserById(Long id);

    VipUserVO createVipUser(VipUserRequestVO request);

    VipUserVO updateVipUser(Long id, VipUserRequestVO request);

    void deleteVipUser(Long id);
}
