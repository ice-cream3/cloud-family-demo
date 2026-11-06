package com.kfpd.cloud.manager.service.impl;

import java.util.*;
import com.kfpd.cloud.common.showcase.CommerceRules;
import com.kfpd.cloud.common.config.datasource.MultiDataSourceNames;
import com.kfpd.cloud.manager.dao.cloud.DictionaryDao;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MerchantTypeAdminService {
    private final DictionaryDao dao;
    private final DictionaryService dictionaries;
    public MerchantTypeAdminService(DictionaryDao dao, DictionaryService dictionaries) {
        this.dao=dao; this.dictionaries=dictionaries;
    }
    public List<Map<String,Object>> categories() { return dao.merchantCategories(); }
    @Transactional(transactionManager=MultiDataSourceNames.FA_CLOUD_TRANSACTION_MANAGER)
    public Map<String,Object> save(Long id, Map<String,Object> input) {
        CommerceRules.require(input!=null,"请填写商家类型资料");
        var before=id==null ? null : dao.merchantItem(id);
        if(id!=null && before==null) throw SystemManagementSupport.notFound("商家类型不存在或尚未关联字典");
        String category=CommerceRules.typeCode(input.get("categoryCode"));
        var type=dao.lockType(category);
        CommerceRules.require(type!=null && ("ECOMMERCE".equals(type.get("parentTypeCode")) ||
            (before!=null && "MERCHANT_TYPE".equals(category))),"请选择电商经营大类");
        if(before!=null) CommerceRules.require(category.equals(before.get("typeCode")),"所属大类创建后不可修改");
        Map<String,Object> record=new HashMap<>(input);
        record.put("typeCode",category);
        record.put("itemCode",CommerceRules.typeCode(input.get("typeCode")));
        dictionaries.save("items",before==null ? null : ((Number)before.get("id")).longValue(),record);
        return dao.merchantType((String)record.get("itemCode"));
    }
    @Transactional(transactionManager=MultiDataSourceNames.FA_CLOUD_TRANSACTION_MANAGER)
    public Map<String,Object> setStatus(Long id,String status) {
        CommerceRules.require("ENABLED".equals(status) || "DISABLED".equals(status),"状态仅支持启用或停用");
        var item=dao.merchantItem(id);
        if(item==null) throw SystemManagementSupport.notFound("商家类型不存在或尚未关联字典");
        if("ENABLED".equals(status)) {
            var category=dao.lockType((String)item.get("typeCode"));
            CommerceRules.require(category!=null && !"DISABLED".equals(category.get("status")),"请先启用所属经营大类");
            if(category.get("parentTypeCode")!=null) {
                var parent=dao.lockType((String)category.get("parentTypeCode"));
                CommerceRules.require(parent!=null && !"DISABLED".equals(parent.get("status")),"请先启用上级电商大类");
            }
        }
        Long dictionaryId=((Number)item.get("id")).longValue();
        var existing=dao.find("items",dictionaryId);
        if(existing==null) throw SystemManagementSupport.notFound("字典项不存在");
        var values=new HashMap<>(existing);values.put("status",status);
        dictionaries.save("items",dictionaryId,values);
        return dao.merchantType((String)item.get("itemCode"));
    }
    @Transactional(transactionManager=MultiDataSourceNames.FA_CLOUD_TRANSACTION_MANAGER)
    public void delete(Long id) {
        var item=dao.merchantItem(id);
        if(item==null) throw SystemManagementSupport.notFound("商家类型不存在或尚未关联字典");
        dictionaries.delete("items",((Number)item.get("id")).longValue());
    }
}
