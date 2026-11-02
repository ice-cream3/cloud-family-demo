package com.kfpd.cloud.partner.controller;

import java.util.List;
import java.util.Map;
import com.kfpd.cloud.common.exception.BusinessException;
import com.kfpd.cloud.common.exception.ErrorCode;
import com.kfpd.cloud.common.web.ApiResponse;
import com.kfpd.cloud.partner.dao.cloud.ShowcaseDao;
import com.kfpd.cloud.partner.pojo.vo.PageVO;
import com.kfpd.cloud.partner.pojo.vo.ShowcaseQueryVO;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users/showcase")
public class ShowcaseController {
    private final ShowcaseDao dao;
    public ShowcaseController(ShowcaseDao dao) { this.dao = dao; }

    @PostMapping("/page")
    public ApiResponse<PageVO<Map<String, Object>>> page(@RequestBody(required = false) ShowcaseQueryVO query) {
        int pageNum = query == null || query.pageNum() == null ? 1 : Math.max(1, Math.min(1000000, query.pageNum()));
        int pageSize = query == null || query.pageSize() == null ? 12 : Math.max(1, Math.min(100, query.pageSize()));
        String category = query == null ? null : trim(query.category(), 32);
        String keyword = query == null ? null : trim(query.keyword(), 100);
        return ApiResponse.success(new PageVO<>(dao.count(category, keyword), pageNum, pageSize,
                dao.page(category, keyword, (pageNum - 1) * pageSize, pageSize)));
    }

    @PostMapping("/categories")
    public ApiResponse<List<String>> categories() { return ApiResponse.success(dao.categories()); }

    @PostMapping("/detail/{id}")
    public ApiResponse<Map<String, Object>> detail(@PathVariable Long id) {
        Map<String, Object> item = dao.detail(id);
        if (item == null) throw new BusinessException(ErrorCode.COMMON_NOT_FOUND, "精品内容不存在或已下架");
        return ApiResponse.success(item);
    }

    private String trim(String value, int limit) {
        if (value == null || value.isBlank()) return null;
        if (value.trim().length() > limit) throw new BusinessException(ErrorCode.COMMON_BAD_REQUEST, "查询条件过长");
        return value.trim();
    }
}
