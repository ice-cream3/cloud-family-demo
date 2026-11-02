package com.kfpd.cloud.partner.dao.cloud;
import java.util.List;
import java.util.Map;
import org.apache.ibatis.annotations.Param;

public interface CommerceDao {
    long countShops(@Param("keyword") String keyword);
    List<Map<String,Object>> shops(@Param("keyword") String keyword,@Param("offset") int offset,@Param("pageSize") int pageSize);
    Map<String,Object> shop(@Param("id") Long id);
    long countProducts(@Param("shopId") Long shopId,@Param("keyword") String keyword,@Param("category") String category);
    List<Map<String,Object>> products(@Param("shopId") Long shopId,@Param("keyword") String keyword,@Param("category") String category,@Param("offset") int offset,@Param("pageSize") int pageSize);
    List<String> categories(@Param("shopId") Long shopId);
    Map<String,Object> product(@Param("id") Long id);
    Map<String,Object> mineShop(@Param("username") String username);
    int saveShop(@Param("username") String username,@Param("record") Map<String,Object> record);
    long countMine(@Param("username") String username,@Param("keyword") String keyword,@Param("status") String status);
    List<Map<String,Object>> mine(@Param("username") String username,@Param("keyword") String keyword,@Param("status") String status,@Param("offset") int offset,@Param("pageSize") int pageSize);
    int create(@Param("username") String username,@Param("record") Map<String,Object> record);
    Map<String,Object> publishState(@Param("username") String username,@Param("id") Long id);
    int update(@Param("username") String username,@Param("id") Long id,@Param("record") Map<String,Object> record);
    int delete(@Param("username") String username,@Param("id") Long id);
}
