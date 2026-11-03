package com.kfpd.cloud.manager.service.impl;

import java.util.HashMap;
import java.util.Map;
import com.kfpd.cloud.common.exception.BusinessException;
import com.kfpd.cloud.manager.dao.cloud.UserFeatureAdminDao;
import com.kfpd.cloud.manager.dao.cloud.CommerceTypeDao;
import com.kfpd.cloud.manager.service.OperationLogService;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class ShowcaseAdminTests {
    private final UserFeatureAdminDao dao = mock(UserFeatureAdminDao.class, withSettings().mockMaker("mock-maker-subclass"));
    private final OperationLogService logs = mock(OperationLogService.class, withSettings().mockMaker("mock-maker-subclass"));
    private final CommerceTypeDao types=mock(CommerceTypeDao.class,withSettings().mockMaker("mock-maker-subclass"));
    private final UserFeatureAdminServiceImpl service = new UserFeatureAdminServiceImpl(dao, logs,types);
    @org.junit.jupiter.api.BeforeEach void typeSetup() {
        when(types.productShop(3L)).thenReturn(7L);
        when(dao.findCommerceTypeCode(2L)).thenReturn("TEST_TYPE");
        when(types.lockShop(7L)).thenReturn(Map.of("id",7L));
        when(types.allowedType(anyMap())).thenReturn(Map.of("id",1L,"typeCode","TYPE_1","name","分类"));
    }

    private Map<String, Object> item() {
        return new HashMap<>(Map.of("title", " 精选内容 ", "category", "精选", "summary", "摘要", "content", "正文\n第二段"));
    }

    @Test void createsDraftWithSafeDefaultsAndTrimsText() {
        when(dao.insertShowcase(anyMap())).thenReturn(1);
        Map<String, Object> result = service.create("showcase", item());
        assertThat(result).containsEntry("title", "精选内容").containsEntry("status", "DRAFT").containsEntry("sortOrder", 0);
        assertThat(result.get("content")).isEqualTo("正文\n第二段");
        verify(dao).insertShowcase(result);
    }

    @Test void rejectsInvalidStatusAndEmptyContentWithoutWriting() {
        Map<String, Object> item = item();
        item.put("status", "ACTIVE");
        assertThatThrownBy(() -> service.create("showcase", item)).isInstanceOf(BusinessException.class);
        item.remove("status"); item.put("content", " ");
        assertThatThrownBy(() -> service.create("showcase", item)).isInstanceOf(BusinessException.class);
        verify(dao, never()).insertShowcase(anyMap());
    }

    @Test void rejectsUnsafeCoverAndFractionalSort() {
        Map<String, Object> item = item();
        item.put("coverUrl", "javascript:alert(1)");
        assertThatThrownBy(() -> service.create("showcase", item)).isInstanceOf(BusinessException.class);
        item.put("coverUrl", "https://example.com/cover.png"); item.put("sortOrder", 1.5);
        assertThatThrownBy(() -> service.create("showcase", item)).isInstanceOf(BusinessException.class);
        verify(dao, never()).insertShowcase(anyMap());
    }

    @Test void rejectsOverlongTitleAndSupportsPublish() {
        Map<String, Object> item = item(); item.put("title", "x".repeat(101));
        assertThatThrownBy(() -> service.create("showcase", item)).isInstanceOf(BusinessException.class);
        item.put("title", "精品"); item.put("status", "PUBLISHED");
        when(dao.updateShowcase(eq(7L), anyMap())).thenReturn(1);
        assertThat(service.update("showcase", 7L, item)).containsEntry("status", "PUBLISHED").containsEntry("id", 7L);
    }

    @Test void protectsNonEmptyShopDeletion() {
        when(dao.countCommerceShopProducts(7L)).thenReturn(2L);
        assertThatThrownBy(() -> service.delete("commerce-shops",7L)).isInstanceOf(BusinessException.class);
        verify(dao,never()).deleteCommerceShop(anyLong());
    }

    @Test void commerceProductValidatesAdminFields() {
        Map<String,Object> input=item();input.put("price","19.90");input.put("typeId",1);input.put("shopId",7);input.put("moderationStatus","BLOCKED");input.put("moderationReason","商品信息不完整，请补充后联系管理员");
        when(dao.updateCommerceProduct(eq(3L),anyMap())).thenReturn(1);
        assertThat(service.update("commerce-products",3L,input)).containsEntry("moderationStatus","BLOCKED").containsEntry("shopId",7L).containsEntry("status","OFFLINE");
        input.put("moderationStatus","INVALID");
        assertThatThrownBy(() -> service.update("commerce-products",3L,input)).isInstanceOf(BusinessException.class);
    }

    @Test void systemBlockRequiresReasonAndRestoreClearsReason() {
        Map<String,Object> input=item();input.put("price","19.90");input.put("typeId",1);input.put("shopId",7);input.put("moderationStatus","BLOCKED");
        assertThatThrownBy(() -> service.update("commerce-products",3L,input)).isInstanceOf(BusinessException.class).hasMessageContaining("系统停用原因");
        input.put("moderationReason","x".repeat(501));
        assertThatThrownBy(() -> service.update("commerce-products",3L,input)).isInstanceOf(BusinessException.class);
        input.put("moderationStatus","ACTIVE");input.put("moderationReason","旧原因");
        when(dao.updateCommerceProduct(eq(3L),anyMap())).thenReturn(1);
        assertThat(service.update("commerce-products",3L,input)).containsEntry("moderationReason",null);
    }

    @Test void typeDisableRefreshesProductsAndReferencedTypeCannotBeDeleted() {
        when(dao.updateCommerceType(eq(2L),anyMap())).thenReturn(1);
        service.update("commerce-types",2L,Map.of("typeCode","TEST_TYPE","name","食品","status","DISABLED","sortOrder",0));
        verify(dao).refreshCommerceTypeProducts(eq(2L),argThat(row->"DISABLED".equals(row.get("status"))));
        when(dao.countCommerceTypeUsage(2L)).thenReturn(1L);
        assertThatThrownBy(()->service.delete("commerce-types",2L)).isInstanceOf(BusinessException.class);
        verify(dao,never()).deleteCommerceType(anyLong());
    }

    @Test void typeCodeRequiredAndImmutableButDisplayNameMayChange() {
        assertThatThrownBy(()->service.create("commerce-types",Map.of("name","食品"))).isInstanceOf(BusinessException.class);
        assertThatThrownBy(()->service.update("commerce-types",2L,Map.of("typeCode","CHANGED","name","食品"))).isInstanceOf(BusinessException.class).hasMessageContaining("不可修改");
        when(dao.updateCommerceType(eq(2L),anyMap())).thenReturn(1);
        assertThat(service.update("commerce-types",2L,Map.of("typeCode","test_type","name","展示新名称"))).containsEntry("typeCode","TEST_TYPE").containsEntry("name","展示新名称");
    }

    @Test void missingUpdateAndDeleteAreReported() {
        assertThatThrownBy(() -> service.update("showcase", 99L, item())).isInstanceOf(BusinessException.class);
        assertThatThrownBy(() -> service.delete("showcase", 99L)).isInstanceOf(BusinessException.class);
    }
}
