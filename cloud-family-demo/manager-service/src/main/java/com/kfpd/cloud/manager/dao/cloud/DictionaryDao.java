package com.kfpd.cloud.manager.dao.cloud;
import java.util.*;
import org.apache.ibatis.annotations.Param;
public interface DictionaryDao {
    List<Map<String,Object>> merchantCategories();
    Map<String,Object> merchantItem(@Param("id") Long id);
    Map<String,Object> merchantType(@Param("code") String code);
    int syncMerchantTypes();
    long countChildren(@Param("typeCode") String typeCode);
    long conflictingMerchantItems(Map<String,Object> record);
    int refreshMerchantProducts();
    long merchantReferences(@Param("itemCode") String itemCode);
    int deleteMerchantType(@Param("itemCode") String itemCode);
    long count(Map<String,Object> query);
    List<Map<String,Object>> page(Map<String,Object> query);
    Map<String,Object> find(@Param("kind") String kind,@Param("id") Long id);
    Map<String,Object> lockType(@Param("typeCode") String typeCode);
    long countItems(@Param("typeCode") String typeCode);
    int insert(Map<String,Object> record);
    int update(Map<String,Object> record);
    int delete(@Param("kind") String kind,@Param("id") Long id);
    List<Map<String,Object>> types();
}
