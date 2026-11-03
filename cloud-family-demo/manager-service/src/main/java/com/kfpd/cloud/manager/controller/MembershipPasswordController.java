package com.kfpd.cloud.manager.controller;

import com.kfpd.cloud.common.web.ApiResponse;
import com.kfpd.cloud.common.exception.BusinessException;
import com.kfpd.cloud.common.exception.ErrorCode;
import com.kfpd.cloud.manager.dao.cloud.UserFeatureAdminDao;
import com.kfpd.cloud.manager.pojo.vo.PasswordResetRequestVO;
import com.kfpd.cloud.manager.pojo.vo.VipUserVO;
import com.kfpd.cloud.manager.service.VipUserService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/manager/partner/features/memberships")
public class MembershipPasswordController {
    private final UserFeatureAdminDao memberships;
    private final VipUserService users;

    public MembershipPasswordController(UserFeatureAdminDao memberships, VipUserService users) {
        this.memberships=memberships;
        this.users=users;
    }

    @PostMapping("/password/reset/{id}")
    @PreAuthorize("hasAuthority('partner:membership:reset-password')")
    public ApiResponse<VipUserVO> resetPassword(@PathVariable Long id,
                                               @Valid @RequestBody PasswordResetRequestVO request) {
        // Membership IDs and account IDs belong to different tables.
        Long userId=memberships.findMembershipUserId(id);
        if(userId==null) throw new BusinessException(ErrorCode.COMMON_NOT_FOUND,"会员记录或关联账号不存在");
        return ApiResponse.success(users.resetVipUserPassword(userId,request));
    }
}
