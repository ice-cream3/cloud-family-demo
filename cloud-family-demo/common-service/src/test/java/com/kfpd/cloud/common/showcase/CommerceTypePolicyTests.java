package com.kfpd.cloud.common.showcase;
import java.util.*;
import com.kfpd.cloud.common.exception.BusinessException;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;

class CommerceTypePolicyTests {
    static class Store implements CommerceTypeStore {
        List<Map<String,Object>> locked=List.of(Map.of("id",1L,"typeCode","FOOD","status","ENABLED"));
        Map<String,Object> allowed=Map.of("id",1L,"typeCode","FOOD","name","食品");
        Map<String,Object> lastAllowedQuery; boolean cleared,offlined;Map<String,Object> inserted;
        public List<Map<String,Object>> available(){return List.of();}
        public List<Map<String,Object>> shopTypes(Map<String,Object> q){return List.of(Map.of("shopId",7L,"id",1L,"typeCode","FOOD","name","食品","status","ENABLED"));}
        public Map<String,Object> lockShop(Long id){return Map.of("id",id);}
        public List<Map<String,Object>> lockTypes(Map<String,Object> q){return locked;}
        public Map<String,Object> allowedType(Map<String,Object> q){lastAllowedQuery=q;return allowed;}
        public int clearShopTypes(Long id){cleared=true;return 1;}
        public int insertShopTypes(Map<String,Object> record){inserted=record;return 1;}
        public int offlineUnassigned(Long id){offlined=true;return 1;}
        public Long productShop(Long id){return 7L;}
    }
    @Test void typesMustBeNonemptyListAndAreDeduplicated(){
        assertThatThrownBy(()->CommerceTypePolicy.codes(null)).isInstanceOf(BusinessException.class);
        assertThatThrownBy(()->CommerceTypePolicy.codes(List.of())).isInstanceOf(BusinessException.class);
        assertThat(CommerceTypePolicy.codes(List.of("TOYS","FOOD","TOYS"))).containsExactly("FOOD","TOYS");
    }
    @Test void disabledTypesRejectAssignmentBeforeChangingRelations(){
        var store=new Store();store.locked=List.of(Map.of("id",1L,"status","DISABLED"));
        assertThatThrownBy(()->CommerceTypePolicy.assign(store,7L,List.of("FOOD"))).isInstanceOf(BusinessException.class);
        assertThat(store.cleared).isFalse();
    }
    @Test void assignmentOfflinesProductsNoLongerInScope(){
        var store=new Store();CommerceTypePolicy.assign(store,7L,List.of("FOOD"));
        assertThat(store.inserted).containsEntry("shopId",7L).containsEntry("codes",List.of("FOOD"));assertThat(store.offlined).isTrue();
    }
    @Test void productScopeCannotBeSpoofedByCategory(){
        var store=new Store();var input=new HashMap<String,Object>(Map.of("title","商品","category","伪造名称","typeId",1,"summary","简介","content","详情","price","9.90"));
        assertThat(CommerceTypePolicy.product(store,7L,input)).containsEntry("typeId",1L).containsEntry("category","食品");
        store.allowed=null;assertThatThrownBy(()->CommerceTypePolicy.product(store,7L,input)).isInstanceOf(BusinessException.class);
    }
    @Test void typeCodeIsCanonicalAndTakesPrecedenceOverLegacyId() {
        var store=new Store();
        var input=new HashMap<String,Object>(Map.of("title","商品","category","仅展示","typeCode"," food ","typeId",999,"summary","简介","content","详情","price","9.90"));
        assertThat(CommerceTypePolicy.product(store,7L,input)).containsEntry("typeId",1L).containsEntry("typeCode","FOOD");
        assertThat(store.lastAllowedQuery).containsEntry("typeCode","FOOD").doesNotContainKey("typeId");
        assertThat(CommerceTypePolicy.codes(List.of("food","FOOD"))).containsExactly("FOOD");
        assertThatThrownBy(()->CommerceTypePolicy.codes(List.of("invalid-code"))).isInstanceOf(BusinessException.class);
    }
    @Test void relationWritesOnlyUseCodesAndRejectsNumericIds() {
        var store=new Store();CommerceTypePolicy.assign(store,7L,List.of("FOOD"));
        assertThat(store.inserted).containsEntry("codes",List.of("FOOD")).doesNotContainKey("ids");
        assertThatThrownBy(()->CommerceTypePolicy.assign(new Store(),7L,List.of(1))).isInstanceOf(BusinessException.class);
        var byCode=new Store();CommerceTypePolicy.assign(byCode,7L,List.of("FOOD"));
        assertThat(byCode.inserted).containsEntry("codes",List.of("FOOD")).doesNotContainKey("ids");
    }
    @Test void batchAttachmentIncludesIdsAndNames(){
        var store=new Store();Map<String,Object> shop=new HashMap<>(Map.of("id",7L));CommerceTypePolicy.attach(store,List.of(shop));
        assertThat(shop).containsEntry("typeCodes",List.of("FOOD")).containsEntry("typeNames","食品");
    }
}
