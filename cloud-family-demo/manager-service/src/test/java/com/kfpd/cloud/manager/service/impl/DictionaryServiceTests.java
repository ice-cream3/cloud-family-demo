package com.kfpd.cloud.manager.service.impl;
import java.util.*;
import com.kfpd.cloud.manager.dao.cloud.DictionaryDao;
import com.kfpd.cloud.manager.service.OperationLogService;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DuplicateKeyException;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;
import static org.assertj.core.api.Assertions.*;
class DictionaryServiceTests {
    private final DictionaryDao dao=mock(DictionaryDao.class,withSettings().mockMaker("mock-maker-subclass"));
    private final OperationLogService logs=mock(OperationLogService.class,withSettings().mockMaker("mock-maker-subclass"));
    private final DictionaryService service=new DictionaryService(dao,logs);
    private Map<String,Object> type() { return Map.of("typeCode","reason","name","售后原因"); }
    @Test void rejectsInvalidKindsAndCodesBeforeSql() {
        assertThatThrownBy(()->service.save("sql",null,type())).hasMessageContaining("不存在");
        assertThatThrownBy(()->service.save("types",null,Map.of("typeCode","bad code","name","原因"))).hasMessageContaining("编码");
        verifyNoInteractions(dao);
    }
    @Test void protectsStableTypeAndItemCodes() {
        when(dao.find("types",1L)).thenReturn(Map.of("typeCode","ORIGINAL"));
        assertThatThrownBy(()->service.save("types",1L,type())).hasMessageContaining("不可修改");
        when(dao.lockType("REASON")).thenReturn(Map.of("id",2L));
        when(dao.find("items",3L)).thenReturn(Map.of("typeCode","REASON","itemCode","ORIGINAL"));
        assertThatThrownBy(()->service.save("items",3L,Map.of("typeCode","REASON","itemCode","changed","name","原因"))).hasMessageContaining("不可修改");
        verify(dao,never()).update(anyMap());
    }
    @Test void blocksTypeDeletionWhileItemsRemain() {
        when(dao.find("types",1L)).thenReturn(Map.of("typeCode","REASON"));when(dao.countItems("REASON")).thenReturn(1L);
        assertThatThrownBy(()->service.delete("types",1L)).hasMessageContaining("存在字典项");
        verify(dao,never()).delete(anyString(),anyLong());
    }
    @Test void requiresParentAndReportsDuplicateCodes() {
        when(dao.lockType("REASON")).thenReturn(null);
        assertThatThrownBy(()->service.save("items",null,Map.of("typeCode","REASON","itemCode","DAMAGED","name","破损"))).hasMessageContaining("先创建");
        when(dao.insert(anyMap())).thenThrow(new DuplicateKeyException("duplicate"));
        assertThatThrownBy(()->service.save("types",null,type())).hasMessageContaining("编码已存在");
        verifyNoInteractions(logs);
    }
    @Test void createsNormalizedDictionaryAndLogsIt() {
        when(dao.insert(anyMap())).thenAnswer(invocation->{ Map<String,Object> record=invocation.getArgument(0);record.put("id",9L);return 1; });
        assertThat(service.save("types",null,type())).containsEntry("typeCode","REASON").containsEntry("status","ENABLED");
        verify(logs).recordCreate(eq("SYSTEM"),eq("DICTIONARY_TYPES"),eq(9L),eq("售后原因"),any());
    }
    @Test void merchantDictionaryChangesSynchronizeTypesAndProducts() {
        when(dao.lockType("MERCHANT_TYPE")).thenReturn(Map.of("id",1L));
        when(dao.insert(anyMap())).thenAnswer(call->{ Map<String,Object> r=call.getArgument(0); r.put("id",9L); return 1; });
        service.save("items",null,Map.of("typeCode","MERCHANT_TYPE","itemCode","FRUIT_STORE","name","水果店"));
        verify(dao).syncMerchantTypes();
        verify(dao).refreshMerchantProducts();
    }
    @Test void disablingMerchantDictionarySynchronizesEffectiveStatus() {
        when(dao.find("types",1L)).thenReturn(Map.of("typeCode","MERCHANT_TYPE"));
        service.save("types",1L,Map.of("typeCode","MERCHANT_TYPE","name","商家类型","status","DISABLED"));
        verify(dao).syncMerchantTypes();
        verify(dao).refreshMerchantProducts();
    }
    @Test void referencedMerchantItemsCannotBeDeleted() {
        when(dao.find("items",2L)).thenReturn(Map.of("typeCode","MERCHANT_TYPE","itemCode","FRUIT_STORE"));
        when(dao.merchantReferences("FRUIT_STORE")).thenReturn(1L);
        assertThatThrownBy(()->service.delete("items",2L)).hasMessageContaining("已引用");
        verify(dao,never()).deleteMerchantType(anyString());
        verify(dao,never()).delete(anyString(),anyLong());
    }
    @Test void foodMerchantItemsSynchronizeAndRejectDuplicateCodesAcrossCategories() {
        when(dao.lockType("FOOD")).thenReturn(Map.of("parentTypeCode","ECOMMERCE"));
        when(dao.insert(anyMap())).thenAnswer(call->{ Map<String,Object> r=call.getArgument(0);r.put("id",10L);return 1; });
        var item=Map.<String,Object>of("typeCode","FOOD","itemCode","RESTAURANT","name","餐厅");
        service.save("items",null,item);
        verify(dao).syncMerchantTypes();
        when(dao.conflictingMerchantItems(anyMap())).thenReturn(1L);
        assertThatThrownBy(()->service.save("items",null,item)).hasMessageContaining("其他电商分类");
    }
    @Test void rejectsSelfParentAndMoreThanTwoTypeLevels() {
        assertThatThrownBy(()->service.save("types",null,Map.of("typeCode","FOOD","parentTypeCode","FOOD","name","食品类"))).hasMessageContaining("自身");
        when(dao.lockType("FOOD")).thenReturn(Map.of("parentTypeCode","ECOMMERCE"));
        assertThatThrownBy(()->service.save("types",null,Map.of("typeCode","CHILD","parentTypeCode","FOOD","name","子类"))).hasMessageContaining("两级");
    }
    @Test void protectsCategoriesWithChildrenAndRejectsDirectItems() {
        when(dao.find("types",1L)).thenReturn(Map.of("typeCode","ECOMMERCE"));
        when(dao.countChildren("ECOMMERCE")).thenReturn(1L);
        assertThatThrownBy(()->service.delete("types",1L)).hasMessageContaining("存在子类型");
        when(dao.lockType("ECOMMERCE")).thenReturn(Map.of("id",1L));
        assertThatThrownBy(()->service.save("items",null,Map.of("typeCode","ECOMMERCE","itemCode","FOOD","name","食品类"))).hasMessageContaining("先创建子类型");
    }
    @Test void referencedFoodMerchantItemCannotBeDeleted() {
        when(dao.find("items",2L)).thenReturn(Map.of("typeCode","FOOD","itemCode","RESTAURANT"));
        when(dao.lockType("FOOD")).thenReturn(Map.of("parentTypeCode","ECOMMERCE"));
        when(dao.merchantReferences("RESTAURANT")).thenReturn(1L);
        assertThatThrownBy(()->service.delete("items",2L)).hasMessageContaining("已引用");
        verify(dao,never()).deleteMerchantType(anyString());
    }
}
