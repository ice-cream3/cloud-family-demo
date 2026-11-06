package com.kfpd.cloud.manager.controller;
import java.util.List;
import java.util.Map;
import com.kfpd.cloud.common.web.ApiResponse;
import com.kfpd.cloud.manager.dao.cloud.CommerceTypeDao;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/manager/partner/shop-types")
public class CommerceTypeAdminController {
    private final CommerceTypeDao types;
    private final com.kfpd.cloud.manager.service.impl.MerchantTypeAdminService admin;
    public CommerceTypeAdminController(CommerceTypeDao types,com.kfpd.cloud.manager.service.impl.MerchantTypeAdminService admin) { this.types=types;this.admin=admin; }
    @PostMapping("/categories")
    @PreAuthorize("hasAuthority('partner:shop-type:view') or hasAuthority('partner:shop-type:add') or hasAuthority('partner:shop-type:edit')")
    public ApiResponse<List<Map<String,Object>>> categories() { return ApiResponse.success(admin.categories()); }
    @PostMapping("/status/{id}")
    @PreAuthorize("hasAuthority('partner:shop-type:edit')")
    public ApiResponse<Map<String,Object>> status(@PathVariable Long id,@RequestBody Map<String,Object> request) {
        return ApiResponse.success(admin.setStatus(id,request.get("status") instanceof String status ? status : null));
    }
    @PostMapping("/options")
    @PreAuthorize("hasAuthority('partner:showcase:view') or hasAuthority('partner:shop-type:view')")
    public ApiResponse<List<Map<String,Object>>> options(@RequestBody(required=false) Map<String,Object> query) {
        if(query!=null && query.get("shopId")!=null) {
            long shopId=com.kfpd.cloud.common.showcase.CommerceRules.id(query.get("shopId"));
            return ApiResponse.success(types.shopTypes(Map.of("shopIds",List.of(shopId))).stream().filter(t->"ENABLED".equals(t.get("status"))).toList());
        }
        return ApiResponse.success(types.available());
    }
}
