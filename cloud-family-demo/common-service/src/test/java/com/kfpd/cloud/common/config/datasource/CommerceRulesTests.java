package com.kfpd.cloud.common.config.datasource;
import java.util.HashMap;
import java.util.Map;
import com.kfpd.cloud.common.showcase.CommerceRules;
import com.kfpd.cloud.common.exception.BusinessException;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;

class CommerceRulesTests {
    private Map<String,Object> product() { return new HashMap<>(Map.of("title","商品","category","分类","summary","简介","content","详情","price","12.30")); }
    @Test void preservesMoneyAndDefaultsWithoutAcceptingSellerModerationOrOwner() {
        var input=product(); input.put("moderationStatus","ACTIVE");input.put("moderationReason","试图清除原因"); input.put("ownerUsername","other");input.put("shopId",9);
        var result=CommerceRules.product(input);
        assertThat(result.get("price").toString()).isEqualTo("12.30");
        assertThat(result).containsEntry("status","DRAFT").containsEntry("stock",0).doesNotContainKeys("moderationStatus","moderationReason","ownerUsername","shopId");
    }
    @Test void rejectsInvalidMoney() { for(String value:new String[]{"-1","1.001","NaN","10000000000.00"}) { var input=product();input.put("price",value);assertThatThrownBy(()->CommerceRules.product(input)).isInstanceOf(BusinessException.class); } }
    @Test void rejectsNegativeAndFractionalStock() { for(Object value:new Object[]{-1,"1.5","2147483648"}) { var input=product();input.put("stock",value);assertThatThrownBy(()->CommerceRules.product(input)).isInstanceOf(BusinessException.class); } }
    @Test void rejectsUnsafeImageAndInvalidStatus() { var input=product();input.put("coverUrl","javascript:alert(1)");assertThatThrownBy(()->CommerceRules.product(input)).isInstanceOf(BusinessException.class);input.remove("coverUrl");input.put("status","ACTIVE");assertThatThrownBy(()->CommerceRules.product(input)).isInstanceOf(BusinessException.class); }
    @Test void shopIgnoresOwnerAndAdminControlFields() { var result=CommerceRules.shop(Map.of("name","店铺","description","介绍","status","ACTIVE","ownerUsername","other","sortOrder",99)); assertThat(result).doesNotContainKeys("status","ownerUsername","sortOrder"); }
}
