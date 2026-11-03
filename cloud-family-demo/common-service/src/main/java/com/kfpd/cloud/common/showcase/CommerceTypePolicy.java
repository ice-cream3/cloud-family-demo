package com.kfpd.cloud.common.showcase;
import java.util.*;

public final class CommerceTypePolicy {
    private CommerceTypePolicy() { }
    public static List<String> codes(Object raw) {
        CommerceRules.require(raw instanceof List<?>,"请选择至少一个商家类型");
        List<?> list=(List<?>)raw;
        CommerceRules.require(!list.isEmpty() && list.size()<=20,"请选择1至20个商家类型");
        return list.stream().map(CommerceRules::typeCode).distinct().sorted().toList();
    }
    public static void assign(CommerceTypeStore store,long shopId,Object rawCodes) {
        List<String> requested=codes(rawCodes);
        CommerceRules.require(store.lockShop(shopId)!=null,"商家不存在");
        List<Map<String,Object>> types=store.lockTypes(Map.of("codes",requested));
        CommerceRules.require(types.size()==requested.size() && types.stream().allMatch(t->"ENABLED".equals(t.get("status"))),"请选择已启用的商家类型");
        List<String> codes=types.stream().map(t->String.valueOf(t.get("typeCode"))).sorted().toList();
        store.clearShopTypes(shopId);
        store.insertShopTypes(Map.of("shopId",shopId,"codes",codes));
        store.offlineUnassigned(shopId);
    }
    public static Map<String,Object> product(CommerceTypeStore store,long shopId,Map<String,Object> input) {
        CommerceRules.require(input!=null,"请填写商品信息");
        CommerceRules.require(store.lockShop(shopId)!=null,"商家不存在");
        Map<String,Object> query=new HashMap<>();query.put("shopId",shopId);
        if(input.get("typeCode")!=null) query.put("typeCode",CommerceRules.typeCode(input.get("typeCode")));
        else query.put("typeId",CommerceRules.id(input.get("typeId")));
        Map<String,Object> type=store.allowedType(query);
        CommerceRules.require(type!=null,"商品类型必须属于商家已选择且启用的类型");
        Map<String,Object> values=new HashMap<>(input);
        values.put("category",type.get("name"));
        Map<String,Object> result=CommerceRules.product(values);
        result.put("typeId",((Number)type.get("id")).longValue());
        result.put("typeCode",type.get("typeCode"));
        return result;
    }
    public static void attach(CommerceTypeStore store,List<Map<String,Object>> shops) {
        if(shops.isEmpty()) return;
        List<Long> shopIds=shops.stream().map(s->((Number)s.get("id")).longValue()).toList();
        Map<Long,List<Map<String,Object>>> grouped=new HashMap<>();
        for(var row:store.shopTypes(Map.of("shopIds",shopIds))) {
            long shopId=((Number)row.get("shopId")).longValue();
            grouped.computeIfAbsent(shopId,key->new ArrayList<>()).add(Map.of("id",row.get("id"),"name",row.get("name"),"typeCode",row.get("typeCode"),"status",row.get("status")));
        }
        for(var shop:shops) {
            var types=grouped.getOrDefault(((Number)shop.get("id")).longValue(),List.of());
            shop.put("types",types);
            shop.put("typeCodes",types.stream().map(t->t.get("typeCode")).toList());
            shop.put("typeNames",String.join("、",types.stream().map(t->String.valueOf(t.get("name"))).toList()));
        }
    }
}
