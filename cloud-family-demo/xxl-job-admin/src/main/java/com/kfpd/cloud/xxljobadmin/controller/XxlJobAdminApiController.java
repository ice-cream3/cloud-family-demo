package com.kfpd.cloud.xxljobadmin.controller;

import java.util.List;
import java.util.Map;

import com.kfpd.cloud.xxljobadmin.base.config.XxlJobAdminProperties;
import com.kfpd.cloud.xxljobadmin.pojo.TriggerRequest;
import com.kfpd.cloud.xxljobadmin.service.LightweightXxlJobAdminService;
import com.xxl.job.core.biz.model.HandleCallbackParam;
import com.xxl.job.core.biz.model.RegistryParam;
import com.xxl.job.core.biz.model.ReturnT;

import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.util.StringUtils;

@RestController
@RequestMapping
public class XxlJobAdminApiController {

    private static final String XXL_JOB_ACCESS_TOKEN = "XXL-JOB-ACCESS-TOKEN";

    private final XxlJobAdminProperties properties;
    private final LightweightXxlJobAdminService adminService;

    public XxlJobAdminApiController(XxlJobAdminProperties properties,
                                    LightweightXxlJobAdminService adminService) {
        this.properties = properties;
        this.adminService = adminService;
    }

    @PostMapping("/api/registry")
    public ReturnT<String> registry(@RequestBody RegistryParam param,
                                    @RequestHeader HttpHeaders headers) {
        ReturnT<String> tokenCheck = checkAccessToken(headers);
        return tokenCheck.getCode() == ReturnT.SUCCESS_CODE ? adminService.registry(param) : tokenCheck;
    }

    @PostMapping("/api/registryRemove")
    public ReturnT<String> registryRemove(@RequestBody RegistryParam param,
                                          @RequestHeader HttpHeaders headers) {
        ReturnT<String> tokenCheck = checkAccessToken(headers);
        return tokenCheck.getCode() == ReturnT.SUCCESS_CODE ? adminService.registryRemove(param) : tokenCheck;
    }

    @PostMapping("/api/callback")
    public ReturnT<String> callback(@RequestBody List<HandleCallbackParam> params,
                                    @RequestHeader HttpHeaders headers) {
        ReturnT<String> tokenCheck = checkAccessToken(headers);
        return tokenCheck.getCode() == ReturnT.SUCCESS_CODE ? adminService.callback(params) : tokenCheck;
    }

    @GetMapping("/")
    public Map<String, Object> index() {
        return Map.of(
                "name", "lightweight-xxl-job-admin",
                "registeredExecutors", adminService.registeredExecutors()
        );
    }

    @GetMapping("/executors")
    public Map<String, List<String>> executors() {
        return adminService.registeredExecutors();
    }

    @PostMapping("/jobs/trigger")
    public List<ReturnT<String>> trigger(@RequestBody TriggerRequest request,
                                         @RequestHeader HttpHeaders headers) {
        ReturnT<String> tokenCheck = checkAccessToken(headers);
        return tokenCheck.getCode() == ReturnT.SUCCESS_CODE ? adminService.trigger(request) : List.of(tokenCheck);
    }

    private ReturnT<String> checkAccessToken(HttpHeaders headers) {
        String expectedToken = properties.getAccessToken();
        if (!StringUtils.hasText(expectedToken)) {
            return ReturnT.SUCCESS;
        }
        String actualToken = headers.getFirst(XXL_JOB_ACCESS_TOKEN);
        if (expectedToken.equals(actualToken)) {
            return ReturnT.SUCCESS;
        }
        return new ReturnT<>(ReturnT.FAIL_CODE, "Invalid XXL-JOB access token");
    }
}
