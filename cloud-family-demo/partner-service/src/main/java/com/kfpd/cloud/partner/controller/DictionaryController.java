package com.kfpd.cloud.partner.controller;
import java.util.*;
import com.kfpd.cloud.common.web.ApiResponse;
import com.kfpd.cloud.common.showcase.CommerceRules;
import com.kfpd.cloud.partner.dao.cloud.DictionaryDao;
import org.springframework.web.bind.annotation.*;
@RestController
@RequestMapping("/api/users/dictionaries")
public class DictionaryController {
    private final DictionaryDao dao;
    public DictionaryController(DictionaryDao dao) { this.dao=dao; }
    @PostMapping("/{typeCode}/options")
    public ApiResponse<List<Map<String,Object>>> options(@PathVariable String typeCode) { return ApiResponse.success(dao.options(CommerceRules.typeCode(typeCode))); }
}
