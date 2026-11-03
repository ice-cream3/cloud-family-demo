package com.kfpd.cloud.common.showcase;
import java.util.List;
import java.util.Map;

public interface CommerceTypeStore {
    List<Map<String,Object>> available();
    List<Map<String,Object>> shopTypes(Map<String,Object> query);
    Map<String,Object> lockShop(Long id);
    List<Map<String,Object>> lockTypes(Map<String,Object> query);
    Map<String,Object> allowedType(Map<String,Object> query);
    int clearShopTypes(Long shopId);
    int insertShopTypes(Map<String,Object> record);
    int offlineUnassigned(Long shopId);
    Long productShop(Long id);
}
