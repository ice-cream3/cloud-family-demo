package com.kfpd.cloud.partner.dao.cloud;

import java.util.List;
import java.util.Map;
import org.apache.ibatis.annotations.Param;

public interface ShowcaseDao {
    long count(@Param("category") String category, @Param("keyword") String keyword);
    List<Map<String, Object>> page(@Param("category") String category, @Param("keyword") String keyword,
        @Param("offset") int offset, @Param("pageSize") int pageSize);
    Map<String, Object> detail(@Param("id") Long id);
    List<String> categories();
}
