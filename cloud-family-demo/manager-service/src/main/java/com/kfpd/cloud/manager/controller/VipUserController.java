package com.kfpd.cloud.manager.controller;

import com.kfpd.cloud.common.web.ApiResponse;
import com.kfpd.cloud.manager.pojo.vo.PageVO;
import com.kfpd.cloud.manager.pojo.vo.VipUserPageQueryVO;
import com.kfpd.cloud.manager.pojo.vo.VipUserRequestVO;
import com.kfpd.cloud.manager.pojo.vo.VipUserVO;
import com.kfpd.cloud.manager.service.VipUserService;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/manager/vip-users")
public class VipUserController {

    private final VipUserService vipUserService;

    public VipUserController(VipUserService vipUserService) {
        this.vipUserService = vipUserService;
    }

    @GetMapping
    public ApiResponse<PageVO<VipUserVO>> vipUsers(@ModelAttribute VipUserPageQueryVO query) {
        return ApiResponse.success(vipUserService.findVipUsers(query));
    }

    @GetMapping("/{id}")
    public ApiResponse<VipUserVO> vipUser(@PathVariable Long id) {
        return ApiResponse.success(vipUserService.findVipUserById(id));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<VipUserVO>> createVipUser(@RequestBody VipUserRequestVO request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(vipUserService.createVipUser(request)));
    }

    @PutMapping("/{id}")
    public ApiResponse<VipUserVO> updateVipUser(@PathVariable Long id, @RequestBody VipUserRequestVO request) {
        return ApiResponse.success(vipUserService.updateVipUser(id, request));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> deleteVipUser(@PathVariable Long id) {
        vipUserService.deleteVipUser(id);
        return ApiResponse.success();
    }
}
