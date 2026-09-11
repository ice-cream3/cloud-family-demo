package com.kfpd.cloud.partner.service;

import com.kfpd.cloud.partner.pojo.vo.PageVO;
import com.kfpd.cloud.partner.pojo.vo.UserProfileVO;
import com.kfpd.cloud.partner.pojo.vo.VipUserPageQueryVO;
import com.kfpd.cloud.partner.pojo.vo.VipUserRequestVO;
import com.kfpd.cloud.partner.pojo.vo.VipUserVO;

public interface UserService {

    PageVO<VipUserVO> findVipUsers(VipUserPageQueryVO query);

    VipUserVO findVipUserById(Long id);

    UserProfileVO currentUser(String username, String roles);

    VipUserVO createVipUser(VipUserRequestVO request);

    VipUserVO updateVipUser(Long id, VipUserRequestVO request);

    void deleteVipUser(Long id);
}
