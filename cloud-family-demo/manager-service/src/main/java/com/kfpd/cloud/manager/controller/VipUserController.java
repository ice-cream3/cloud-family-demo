package com.kfpd.cloud.manager.controller;

import com.kfpd.cloud.common.web.ApiResponse;
import com.kfpd.cloud.manager.pojo.vo.PageVO;
import com.kfpd.cloud.manager.pojo.vo.VipUserPageQueryVO;
import com.kfpd.cloud.manager.pojo.vo.VipUserRequestVO;
import com.kfpd.cloud.manager.pojo.vo.VipUserVO;
import com.kfpd.cloud.manager.service.VipUserService;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/manager")
public class VipUserController {

    private final VipUserService vipUserService;

    public VipUserController(VipUserService vipUserService) {
        this.vipUserService = vipUserService;
    }

    @PostMapping("/vip-users/page")
    public ApiResponse<PageVO<VipUserVO>> vipUsers(@ModelAttribute VipUserPageQueryVO query) {
        return ApiResponse.success(vipUserService.findVipUsers(query));
    }

    @PostMapping("/vip-users/detail/{id}")
    public ApiResponse<VipUserVO> vipUser(@PathVariable Long id) {
        return ApiResponse.success(vipUserService.findVipUserById(id));
    }

    @PostMapping("/vip-users")
    public ResponseEntity<ApiResponse<VipUserVO>> createVipUser(@RequestBody VipUserRequestVO request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(vipUserService.createVipUser(request)));
    }

    @PostMapping("/vip-users/update/{id}")
    public ApiResponse<VipUserVO> updateVipUser(@PathVariable Long id, @RequestBody VipUserRequestVO request) {
        return ApiResponse.success(vipUserService.updateVipUser(id, request));
    }

    @PostMapping("/vip-users/delete/{id}")
    public ApiResponse<Void> deleteVipUser(@PathVariable Long id) {
        vipUserService.deleteVipUser(id);
        return ApiResponse.success();
    }
}
