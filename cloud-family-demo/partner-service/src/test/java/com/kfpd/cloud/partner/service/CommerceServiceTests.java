package com.kfpd.cloud.partner.service;
import java.util.Map;
import com.kfpd.cloud.partner.dao.cloud.CommerceDao;
import com.kfpd.cloud.partner.dao.cloud.CommerceTypeDao;
import com.kfpd.cloud.partner.dao.cloud.VipUserDao;
import com.kfpd.cloud.partner.pojo.entity.VipUser;
import com.kfpd.cloud.partner.pojo.vo.CommerceQueryVO;
import com.kfpd.cloud.common.exception.BusinessException;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class CommerceServiceTests {
    private final CommerceDao dao=mock(CommerceDao.class,withSettings().mockMaker("mock-maker-subclass"));
    private final VipUserDao users=mock(VipUserDao.class,withSettings().mockMaker("mock-maker-subclass"));
    private final CommerceTypeDao types=mock(CommerceTypeDao.class,withSettings().mockMaker("mock-maker-subclass"));
    private final CommerceService service=new CommerceService(dao,users,types);
    private void enable() { var user=new VipUser();user.setUsername("alice");user.setStatus("ENABLED");when(users.findByUsername("alice")).thenReturn(user);
        when(dao.publishState("alice",7L)).thenReturn(Map.of("shopId",1L,"shopStatus","ACTIVE","moderationStatus","ACTIVE"));
        when(types.lockShop(1L)).thenReturn(Map.of("id",1L));
        when(types.allowedType(anyMap())).thenReturn(Map.of("id",1L,"typeCode","TYPE_1","name","分类")); }
    private Map<String,Object> product() { return Map.of("title","商品","category","分类","summary","简介","content","详情","price","12.30","ownerUsername","bob","shopId",999,"typeId",1); }
    @Test void rejectsUnknownAndDisabledSellerBeforeWriting() { when(users.findByUsername("alice")).thenReturn(null);assertThatThrownBy(()->service.create("alice",product())).isInstanceOf(BusinessException.class);verifyNoInteractions(dao); }
    @Test void disabledSellerCannotMutate() { enable();users.findByUsername("alice").setStatus("DISABLED");assertThatThrownBy(()->service.create("alice",product())).isInstanceOf(BusinessException.class);verifyNoInteractions(dao); }
    @Test void updateAlwaysUsesAuthenticatedOwnerAndCannotTransferShop() { enable();when(dao.update(eq("alice"),eq(7L),anyMap())).thenReturn(1);service.update("alice",7L,product());verify(dao).update(eq("alice"),eq(7L),argThat(record->!record.containsKey("ownerUsername")&&!record.containsKey("shopId"))); }
    @Test void blockedProductCannotPublishEvenWithForgedModerationField() {
        enable();when(dao.publishState("alice",7L)).thenReturn(Map.of("shopId",1L,"moderationStatus","BLOCKED","shopStatus","ACTIVE"));
        var input=new java.util.HashMap<>(product());input.put("status","PUBLISHED");input.put("moderationStatus","ACTIVE");
        assertThatThrownBy(()->service.update("alice",7L,input)).isInstanceOf(BusinessException.class).hasMessageContaining("商品已被后台停用");
        verify(dao,never()).update(anyString(),anyLong(),anyMap());
    }
    @Test void blockedShopCannotPublishProduct() {
        enable();when(dao.publishState("alice",7L)).thenReturn(Map.of("shopId",1L,"moderationStatus","ACTIVE","shopStatus","BLOCKED"));
        var input=new java.util.HashMap<>(product());input.put("status","PUBLISHED");
        assertThatThrownBy(()->service.update("alice",7L,input)).isInstanceOf(BusinessException.class).hasMessageContaining("店铺已被后台停用");
        verify(dao,never()).update(anyString(),anyLong(),anyMap());
    }
    @Test void activeProductCanPublishButSqlRejectionIsStillRespected() {
        enable();when(dao.publishState("alice",7L)).thenReturn(Map.of("shopId",1L,"moderationStatus","ACTIVE","shopStatus","ACTIVE"));
        var input=new java.util.HashMap<>(product());input.put("status","PUBLISHED");
        when(dao.update(eq("alice"),eq(7L),anyMap())).thenReturn(1);service.update("alice",7L,input);
        when(dao.update(eq("alice"),eq(7L),anyMap())).thenReturn(0);
        assertThatThrownBy(()->service.update("alice",7L,input)).isInstanceOf(BusinessException.class);
    }
    @Test void onlyApprovedActiveShopsCanCreateEvenDraftProducts() {
        enable();
        for (String status : java.util.List.of("PENDING","REJECTED","BLOCKED")) {
            when(dao.mineShop("alice")).thenReturn(Map.of("id",1L,"status",status));
            assertThatThrownBy(()->service.create("alice",product())).isInstanceOf(BusinessException.class).hasMessageContaining("审核通过");
        }
        verify(dao,never()).create(anyString(),anyMap());
        when(dao.mineShop("alice")).thenReturn(Map.of("id",1L,"status","ACTIVE"));
        when(dao.create(eq("alice"),anyMap())).thenReturn(1);
        service.create("alice",product());
        verify(dao).create(eq("alice"),anyMap());
    }
    private Map<String,Object> shopInput() { return Map.of("name","店铺","description","介绍","typeCodes",java.util.List.of("TYPE_1")); }
    @Test void applicationRequiresMerchantTypes() {
        enable();
        for(var input:java.util.List.<Map<String,Object>>of(Map.of("name","店铺","description","介绍"),Map.of("typeCodes",java.util.List.of()))) {
            assertThatThrownBy(()->service.saveShop("alice",input)).isInstanceOf(BusinessException.class).hasMessageContaining("商家类型");
        }
        verify(dao,never()).saveShop(anyString(),anyMap());
    }
    @Test void submittedAndApprovedShopProfilesAreReadOnly() {
        enable();when(dao.mineShop("alice")).thenReturn(Map.of("id",1L));
        for(String status:java.util.List.of("PENDING","ACTIVE","BLOCKED")) {
            when(types.lockShop(1L)).thenReturn(Map.of("id",1L,"status",status));
            assertThatThrownBy(()->service.saveShop("alice",shopInput())).isInstanceOf(BusinessException.class).hasMessageContaining("仅可查看");
        }
        verify(dao,never()).saveShop(anyString(),anyMap());
    }
    private void applicationTypes() {
        when(types.lockShop(1L)).thenReturn(Map.of("id",1L,"status","PENDING"));
        when(types.lockTypes(anyMap())).thenReturn(java.util.List.of(Map.of("typeCode","TYPE_1","status","ENABLED")));
        when(dao.saveShop(eq("alice"),anyMap())).thenReturn(1);
    }
    @Test void newApplicationSavesRequestedMerchantTypes() {
        enable();applicationTypes();
        when(dao.mineShop("alice")).thenReturn(null,new java.util.HashMap<>(Map.of("id",1L,"status","PENDING")));
        assertThat(service.saveShop("alice",shopInput())).containsEntry("status","PENDING");
        verify(types).insertShopTypes(argThat(record->record.get("codes").equals(java.util.List.of("TYPE_1"))));
    }
    @Test void rejectedApplicationCanResubmitRequestedTypes() {
        enable();applicationTypes();
        when(dao.mineShop("alice")).thenReturn(new java.util.HashMap<>(Map.of("id",1L,"status","REJECTED")));
        when(types.lockShop(1L)).thenReturn(Map.of("id",1L,"status","REJECTED"),Map.of("id",1L,"status","PENDING"));
        service.saveShop("alice",shopInput());
        verify(types).insertShopTypes(anyMap());
    }
    @Test void disabledOrUnknownApplicationTypesAreRejected() {
        enable();applicationTypes();
        when(dao.mineShop("alice")).thenReturn(null,new java.util.HashMap<>(Map.of("id",1L,"status","PENDING")));
        when(types.lockTypes(anyMap())).thenReturn(java.util.List.of(Map.of("typeCode","TYPE_1","status","DISABLED")));
        assertThatThrownBy(()->service.saveShop("alice",shopInput())).isInstanceOf(BusinessException.class).hasMessageContaining("已启用");
        verify(types,never()).insertShopTypes(anyMap());
    }
    @Test void foreignDeletionIsRejected() { enable();when(dao.delete("alice",99L)).thenReturn(0);assertThatThrownBy(()->service.delete("alice",99L)).isInstanceOf(BusinessException.class); }
    @Test void blocksHiddenShopsAndProducts() { when(dao.shop(99L)).thenReturn(null);when(dao.product(99L)).thenReturn(null);assertThatThrownBy(()->service.products(99L,null)).isInstanceOf(BusinessException.class);assertThatThrownBy(()->service.product(99L)).isInstanceOf(BusinessException.class); }
    @Test void filtersShopListAndTotalByNormalizedType() {
        service.shops(new CommerceQueryVO(2,12," 超市 ",null,null," retail "));
        verify(dao).shops("超市",java.util.List.of("RETAIL"),12,12);
        verify(dao).countShops("超市",java.util.List.of("RETAIL"));
    }
    @Test void rejectsInvalidShopTypeFilter() {
        assertThatThrownBy(()->service.shops(new CommerceQueryVO(1,12,null,null,null,"bad type!"))).isInstanceOf(BusinessException.class);
        verifyNoInteractions(dao);
    }
    @Test void clampsPagingAndTrimsShopSearch() { service.shops(new CommerceQueryVO(-1,1000," 店铺 ",null,null));verify(dao).shops("店铺",java.util.List.of(),0,100); }
    @Test void normalizesMultipleShopTypesAndSharesFilterWithCount() {
        service.shops(new CommerceQueryVO(2,12," 商家 ",null,null,null,java.util.List.of(" cake_shop ","FRUIT_STORE","CAKE_SHOP")));
        var expected=java.util.List.of("CAKE_SHOP","FRUIT_STORE");
        verify(dao).shops("商家",expected,12,12);
        verify(dao).countShops("商家",expected);
    }
    @Test void rejectsInvalidAndOversizedMultiTypeFilters() {
        assertThatThrownBy(()->service.shops(new CommerceQueryVO(1,12,null,null,null,null,java.util.List.of("BAD!")))).isInstanceOf(BusinessException.class);
        assertThatThrownBy(()->service.shops(new CommerceQueryVO(1,12,null,null,null,null,java.util.Collections.nCopies(101,"CAKE_SHOP")))).isInstanceOf(BusinessException.class);
        verifyNoInteractions(dao);
    }
}
