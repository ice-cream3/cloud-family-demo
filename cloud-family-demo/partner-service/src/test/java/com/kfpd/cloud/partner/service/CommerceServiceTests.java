package com.kfpd.cloud.partner.service;
import java.util.Map;
import com.kfpd.cloud.partner.dao.cloud.CommerceDao;
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
    private final CommerceService service=new CommerceService(dao,users);
    private void enable() { var user=new VipUser();user.setUsername("alice");user.setStatus("ENABLED");when(users.findByUsername("alice")).thenReturn(user); }
    private Map<String,Object> product() { return Map.of("title","商品","category","分类","summary","简介","content","详情","price","12.30","ownerUsername","bob","shopId",999); }
    @Test void rejectsUnknownAndDisabledSellerBeforeWriting() { when(users.findByUsername("alice")).thenReturn(null);assertThatThrownBy(()->service.create("alice",product())).isInstanceOf(BusinessException.class);verifyNoInteractions(dao); }
    @Test void disabledSellerCannotMutate() { enable();users.findByUsername("alice").setStatus("DISABLED");assertThatThrownBy(()->service.create("alice",product())).isInstanceOf(BusinessException.class);verifyNoInteractions(dao); }
    @Test void updateAlwaysUsesAuthenticatedOwnerAndCannotTransferShop() { enable();when(dao.update(eq("alice"),eq(7L),anyMap())).thenReturn(1);service.update("alice",7L,product());verify(dao).update(eq("alice"),eq(7L),argThat(record->!record.containsKey("ownerUsername")&&!record.containsKey("shopId"))); }
    @Test void blockedProductCannotPublishEvenWithForgedModerationField() {
        enable();when(dao.publishState("alice",7L)).thenReturn(Map.of("moderationStatus","BLOCKED","shopStatus","ACTIVE"));
        var input=new java.util.HashMap<>(product());input.put("status","PUBLISHED");input.put("moderationStatus","ACTIVE");
        assertThatThrownBy(()->service.update("alice",7L,input)).isInstanceOf(BusinessException.class).hasMessageContaining("商品已被后台停用");
        verify(dao,never()).update(anyString(),anyLong(),anyMap());
    }
    @Test void blockedShopCannotPublishProduct() {
        enable();when(dao.publishState("alice",7L)).thenReturn(Map.of("moderationStatus","ACTIVE","shopStatus","BLOCKED"));
        var input=new java.util.HashMap<>(product());input.put("status","PUBLISHED");
        assertThatThrownBy(()->service.update("alice",7L,input)).isInstanceOf(BusinessException.class).hasMessageContaining("店铺已被后台停用");
        verify(dao,never()).update(anyString(),anyLong(),anyMap());
    }
    @Test void activeProductCanPublishButSqlRejectionIsStillRespected() {
        enable();when(dao.publishState("alice",7L)).thenReturn(Map.of("moderationStatus","ACTIVE","shopStatus","ACTIVE"));
        var input=new java.util.HashMap<>(product());input.put("status","PUBLISHED");
        when(dao.update(eq("alice"),eq(7L),anyMap())).thenReturn(1);service.update("alice",7L,input);
        when(dao.update(eq("alice"),eq(7L),anyMap())).thenReturn(0);
        assertThatThrownBy(()->service.update("alice",7L,input)).isInstanceOf(BusinessException.class);
    }
    @Test void foreignDeletionIsRejected() { enable();when(dao.delete("alice",99L)).thenReturn(0);assertThatThrownBy(()->service.delete("alice",99L)).isInstanceOf(BusinessException.class); }
    @Test void blocksHiddenShopsAndProducts() { when(dao.shop(99L)).thenReturn(null);when(dao.product(99L)).thenReturn(null);assertThatThrownBy(()->service.products(99L,null)).isInstanceOf(BusinessException.class);assertThatThrownBy(()->service.product(99L)).isInstanceOf(BusinessException.class); }
    @Test void clampsPagingAndTrimsShopSearch() { service.shops(new CommerceQueryVO(-1,1000," 店铺 ",null,null));verify(dao).shops("店铺",0,100); }
}
