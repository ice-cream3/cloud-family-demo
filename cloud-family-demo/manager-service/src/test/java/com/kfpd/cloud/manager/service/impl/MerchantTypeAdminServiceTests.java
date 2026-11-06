package com.kfpd.cloud.manager.service.impl;

import java.util.*;
import com.kfpd.cloud.manager.dao.cloud.DictionaryDao;
import com.kfpd.cloud.manager.service.OperationLogService;
import org.junit.jupiter.api.Test;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;
import static org.assertj.core.api.Assertions.*;

class MerchantTypeAdminServiceTests {
    private final DictionaryDao dao=mock(DictionaryDao.class,withSettings().mockMaker("mock-maker-subclass"));
    private final OperationLogService logs=mock(OperationLogService.class,withSettings().mockMaker("mock-maker-subclass"));
    private final MerchantTypeAdminService service=new MerchantTypeAdminService(dao,new DictionaryService(dao,logs));
    private Map<String,Object> input() { return Map.of("categoryCode","FOOD","typeCode","NEW_SHOP","name","新店铺","status","ENABLED"); }
    private void category() { when(dao.lockType("FOOD")).thenReturn(Map.of("parentTypeCode","ECOMMERCE")); }
    private void existing() {
        category();
        when(dao.merchantItem(7L)).thenReturn(Map.of("id",42L,"typeCode","FOOD","itemCode","NEW_SHOP"));
        when(dao.find("items",42L)).thenReturn(Map.of("typeCode","FOOD","itemCode","NEW_SHOP"));
    }
    @Test void createsDictionaryItemAndReturnsMerchantId() {
        category();
        when(dao.insert(anyMap())).thenAnswer(call->{Map<String,Object> r=call.getArgument(0);r.put("id",42L);return 1;});
        when(dao.merchantType("NEW_SHOP")).thenReturn(Map.of("id",7L,"typeCode","NEW_SHOP","categoryCode","FOOD"));
        assertThat(service.save(null,input())).containsEntry("id",7L).containsEntry("categoryCode","FOOD");
        verify(dao).insert(argThat(r->"FOOD".equals(r.get("typeCode")) && "NEW_SHOP".equals(r.get("itemCode"))));
        verify(dao).syncMerchantTypes();
        verify(dao).refreshMerchantProducts();
    }
    @Test void rejectsUnrelatedDictionaryAndMissingMerchant() {
        when(dao.lockType("FOOD")).thenReturn(Map.of("typeCode","FOOD"));
        assertThatThrownBy(()->service.save(null,input())).hasMessageContaining("电商经营大类");
        when(dao.merchantItem(99L)).thenReturn(null);
        assertThatThrownBy(()->service.save(99L,input())).hasMessageContaining("不存在");
        verify(dao,never()).insert(anyMap());
    }
    @Test void updateProtectsCodeAndCategoryAndSynchronizesDisable() {
        existing();
        var changed=new HashMap<>(input());changed.put("typeCode","CHANGED");
        var differentCode=changed;
        assertThatThrownBy(()->service.save(7L,differentCode)).hasMessageContaining("不可修改");
        when(dao.lockType("FRESH")).thenReturn(Map.of("parentTypeCode","ECOMMERCE"));
        changed=new HashMap<>(input());changed.put("categoryCode","FRESH");
        var differentCategory=changed;
        assertThatThrownBy(()->service.save(7L,differentCategory)).hasMessageContaining("所属大类");
        var disabled=new HashMap<>(input());disabled.put("status","DISABLED");
        service.save(7L,disabled);
        verify(dao).update(argThat(r->Long.valueOf(42L).equals(r.get("id")) && "DISABLED".equals(r.get("status"))));
        verify(dao).refreshMerchantProducts();
    }
    @Test void referencedTypesCannotBeDeleted() {
        existing();when(dao.merchantReferences("NEW_SHOP")).thenReturn(1L);
        assertThatThrownBy(()->service.delete(7L)).hasMessageContaining("已引用");
        verify(dao,never()).deleteMerchantType(anyString());
    }
    @Test void unusedTypeDeletesDictionaryAndMerchantTogether() {
        existing();service.delete(7L);
        verify(dao).deleteMerchantType("NEW_SHOP");
        verify(dao).delete("items",42L);
        verify(logs).recordDelete(eq("SYSTEM"),eq("DICTIONARY_ITEMS"),eq(42L),any(),any());
    }
    @Test void statusActionPreservesStoredMetadataAndSyncsDictionary() {
        existing();
        when(dao.find("items",42L)).thenReturn(Map.of("typeCode","FOOD","itemCode","NEW_SHOP","name","已有名称","description","已有说明","sortOrder",8,"status","ENABLED"));
        service.setStatus(7L,"DISABLED");
        verify(dao).update(argThat(row->"DISABLED".equals(row.get("status")) && "已有名称".equals(row.get("name")) && "已有说明".equals(row.get("description")) && Integer.valueOf(8).equals(row.get("sortOrder"))));
        verify(dao).syncMerchantTypes();verify(dao).refreshMerchantProducts();
    }
    @Test void rejectsInvalidStatusAndDisabledAncestors() {
        assertThatThrownBy(()->service.setStatus(7L,null)).hasMessageContaining("状态");
        assertThatThrownBy(()->service.setStatus(7L,"PUBLISHED")).hasMessageContaining("状态");
        existing();
        when(dao.lockType("FOOD")).thenReturn(Map.of("status","DISABLED","parentTypeCode","ECOMMERCE"));
        assertThatThrownBy(()->service.setStatus(7L,"ENABLED")).hasMessageContaining("所属经营大类");
        when(dao.lockType("FOOD")).thenReturn(Map.of("status","ENABLED","parentTypeCode","ECOMMERCE"));
        when(dao.lockType("ECOMMERCE")).thenReturn(Map.of("status","DISABLED"));
        assertThatThrownBy(()->service.setStatus(7L,"ENABLED")).hasMessageContaining("上级电商大类");
        verify(dao,never()).update(anyMap());
    }
    @Test void enablingUpdatesItemWithoutPublishingProducts() {
        existing();
        when(dao.find("items",42L)).thenReturn(Map.of("typeCode","FOOD","itemCode","NEW_SHOP","name","类型","status","DISABLED"));
        when(dao.lockType("ECOMMERCE")).thenReturn(Map.of("status","ENABLED"));
        service.setStatus(7L,"ENABLED");
        verify(dao).update(argThat(row->"ENABLED".equals(row.get("status"))));
        verify(dao).refreshMerchantProducts();
    }
}
