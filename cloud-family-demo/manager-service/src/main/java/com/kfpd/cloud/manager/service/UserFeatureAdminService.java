package com.kfpd.cloud.manager.service;

import java.util.Map;

import com.kfpd.cloud.manager.pojo.vo.PageQueryVO;
import com.kfpd.cloud.manager.pojo.vo.PageVO;

public interface UserFeatureAdminService {

    PageVO<Map<String, Object>> findPage(String feature, PageQueryVO query);

    Map<String, Object> create(String feature, Map<String, Object> record);

    Map<String, Object> update(String feature, Long id, Map<String, Object> record);

    void delete(String feature, Long id);
}
