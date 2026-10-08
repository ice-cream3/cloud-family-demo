package com.kfpd.cloud.manager.controller;

import java.util.Map;

import com.kfpd.cloud.common.web.ApiResponse;
import com.kfpd.cloud.manager.pojo.vo.PageQueryVO;
import com.kfpd.cloud.manager.pojo.vo.PageVO;
import com.kfpd.cloud.manager.service.UserFeatureAdminService;

import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/manager/partner/features")
public class UserFeatureAdminController {

    private final UserFeatureAdminService userFeatureAdminService;

    public UserFeatureAdminController(UserFeatureAdminService userFeatureAdminService) {
        this.userFeatureAdminService = userFeatureAdminService;
    }

    @PostMapping("/{feature}/page")
    public ApiResponse<PageVO<Map<String, Object>>> page(@PathVariable String feature, @ModelAttribute PageQueryVO query) {
        return ApiResponse.success(userFeatureAdminService.findPage(feature, query));
    }

    @PostMapping("/{feature}")
    public ApiResponse<Map<String, Object>> create(@PathVariable String feature, @RequestBody Map<String, Object> record) {
        return ApiResponse.success(userFeatureAdminService.create(feature, record));
    }

    @PostMapping("/{feature}/update/{id}")
    public ApiResponse<Map<String, Object>> update(@PathVariable String feature,
                                                   @PathVariable Long id,
                                                   @RequestBody Map<String, Object> record) {
        return ApiResponse.success(userFeatureAdminService.update(feature, id, record));
    }

    @PostMapping("/{feature}/delete/{id}")
    public ApiResponse<Void> delete(@PathVariable String feature, @PathVariable Long id) {
        userFeatureAdminService.delete(feature, id);
        return ApiResponse.success();
    }
}
