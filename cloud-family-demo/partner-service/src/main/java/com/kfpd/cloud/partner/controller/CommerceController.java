package com.kfpd.cloud.partner.controller;
import java.util.List;
import java.util.Map;
import com.kfpd.cloud.common.web.ApiResponse;
import com.kfpd.cloud.common.web.GatewayHeaders;
import com.kfpd.cloud.partner.pojo.vo.CommerceQueryVO;
import com.kfpd.cloud.partner.pojo.vo.PageVO;
import com.kfpd.cloud.partner.service.CommerceService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users/commerce")
public class CommerceController {
    private final CommerceService service;
    public CommerceController(CommerceService service) { this.service=service; }
    @PostMapping("/shops/page") public ApiResponse<PageVO<Map<String,Object>>> shops(@RequestBody(required=false) CommerceQueryVO q) { return ApiResponse.success(service.shops(q)); }
    @PostMapping("/shops/{id}") public ApiResponse<Map<String,Object>> shop(@PathVariable Long id) { return ApiResponse.success(service.shop(id)); }
    @PostMapping("/shops/{id}/products/page") public ApiResponse<PageVO<Map<String,Object>>> products(@PathVariable Long id,@RequestBody(required=false) CommerceQueryVO q) { return ApiResponse.success(service.products(id,q)); }
    @PostMapping("/shops/{id}/categories") public ApiResponse<List<String>> categories(@PathVariable Long id) { return ApiResponse.success(service.categories(id)); }
    @PostMapping("/products/detail/{id}") public ApiResponse<Map<String,Object>> detail(@PathVariable Long id) { return ApiResponse.success(service.product(id)); }
    @PostMapping("/mine/shop") public ApiResponse<Map<String,Object>> mineShop(@RequestHeader(GatewayHeaders.USER_NAME) String username) { return ApiResponse.success(service.mineShop(username)); }
    @PostMapping("/mine/shop/save") public ApiResponse<Map<String,Object>> saveShop(@RequestHeader(GatewayHeaders.USER_NAME) String username,@RequestBody Map<String,Object> input) { return ApiResponse.success(service.saveShop(username,input)); }
    @PostMapping("/mine/products/page") public ApiResponse<PageVO<Map<String,Object>>> mine(@RequestHeader(GatewayHeaders.USER_NAME) String username,@RequestBody(required=false) CommerceQueryVO q) { return ApiResponse.success(service.mine(username,q)); }
    @PostMapping("/mine/products") public ApiResponse<Void> create(@RequestHeader(GatewayHeaders.USER_NAME) String username,@RequestBody Map<String,Object> input) { service.create(username,input); return ApiResponse.success(); }
    @PostMapping("/mine/products/update/{id}") public ApiResponse<Void> update(@RequestHeader(GatewayHeaders.USER_NAME) String username,@PathVariable Long id,@RequestBody Map<String,Object> input) { service.update(username,id,input); return ApiResponse.success(); }
    @PostMapping("/mine/products/delete/{id}") public ApiResponse<Void> delete(@RequestHeader(GatewayHeaders.USER_NAME) String username,@PathVariable Long id) { service.delete(username,id); return ApiResponse.success(); }
}
