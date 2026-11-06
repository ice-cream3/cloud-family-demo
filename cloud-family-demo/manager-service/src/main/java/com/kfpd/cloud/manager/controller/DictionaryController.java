package com.kfpd.cloud.manager.controller;
import java.util.*;
import com.kfpd.cloud.common.web.ApiResponse;
import com.kfpd.cloud.manager.pojo.vo.*;
import com.kfpd.cloud.manager.service.impl.DictionaryService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
@RestController
@RequestMapping("/api/manager/system/dictionaries")
public class DictionaryController {
    private final DictionaryService service;
    public DictionaryController(DictionaryService service) { this.service=service; }
    @PostMapping("/type-options") @PreAuthorize("hasAuthority('system:dict:view')")
    public ApiResponse<List<Map<String,Object>>> types() { return ApiResponse.success(service.types()); }
    @PostMapping("/{kind}/page") @PreAuthorize("hasAuthority('system:dict:view')")
    public ApiResponse<PageVO<Map<String,Object>>> page(@PathVariable String kind,@ModelAttribute PageQueryVO q) { return ApiResponse.success(service.page(kind,q)); }
    @PostMapping("/{kind}") @PreAuthorize("hasAuthority('system:dict:add')")
    public ApiResponse<Map<String,Object>> create(@PathVariable String kind,@RequestBody Map<String,Object> r) { return ApiResponse.success(service.save(kind,null,r)); }
    @PostMapping("/{kind}/update/{id}") @PreAuthorize("hasAuthority('system:dict:edit')")
    public ApiResponse<Map<String,Object>> update(@PathVariable String kind,@PathVariable Long id,@RequestBody Map<String,Object> r) { return ApiResponse.success(service.save(kind,id,r)); }
    @PostMapping("/{kind}/delete/{id}") @PreAuthorize("hasAuthority('system:dict:delete')")
    public ApiResponse<Void> delete(@PathVariable String kind,@PathVariable Long id) { service.delete(kind,id);return ApiResponse.success(); }
}
