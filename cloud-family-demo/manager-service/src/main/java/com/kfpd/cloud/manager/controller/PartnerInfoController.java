package com.kfpd.cloud.manager.controller;

import com.kfpd.cloud.common.web.ApiResponse;
import com.kfpd.cloud.manager.pojo.vo.PageVO;
import com.kfpd.cloud.manager.pojo.vo.PasswordResetRequestVO;
import com.kfpd.cloud.manager.pojo.vo.VipUserPageQueryVO;
import com.kfpd.cloud.manager.pojo.vo.VipUserRequestVO;
import com.kfpd.cloud.manager.pojo.vo.VipUserVO;
import com.kfpd.cloud.manager.service.VipUserService;
import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/manager/partner/info")
public class PartnerInfoController {

    private final VipUserService vipUserService;

    public PartnerInfoController(VipUserService vipUserService) {
        this.vipUserService = vipUserService;
    }

    @PostMapping("/page")
    public ApiResponse<PageVO<VipUserVO>> partnerInfoPage(@ModelAttribute VipUserPageQueryVO query) {
        return ApiResponse.success(vipUserService.findVipUsers(query));
    }

    @PostMapping("/detail/{id}")
    public ApiResponse<VipUserVO> partnerInfoDetail(@PathVariable Long id) {
        return ApiResponse.success(vipUserService.findVipUserById(id));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<VipUserVO>> createPartnerInfo(@RequestBody VipUserRequestVO request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(vipUserService.createVipUser(request)));
    }

    @PostMapping("/update/{id}")
    @PreAuthorize("hasAuthority('partner:info:edit')")
    public ApiResponse<VipUserVO> updatePartnerInfo(@PathVariable Long id, @RequestBody VipUserRequestVO request) {
        return ApiResponse.success(vipUserService.updateVipUser(id, request));
    }

    @PostMapping("/password/reset/{id}")
    @PreAuthorize("hasAuthority('partner:info:reset-password')")
    public ApiResponse<VipUserVO> resetPartnerInfoPassword(@PathVariable Long id,
                                                           @Valid @RequestBody(required = false) PasswordResetRequestVO request) {
        return ApiResponse.success(vipUserService.resetVipUserPassword(id, request));
    }

    @PostMapping("/delete/{id}")
    @PreAuthorize("hasAuthority('partner:info:delete')")
    public ApiResponse<Void> deletePartnerInfo(@PathVariable Long id) {
        vipUserService.deleteVipUser(id);
        return ApiResponse.success();
    }
}
