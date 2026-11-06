package com.kfpd.cloud.manager.service.impl;
import java.util.*;
import com.kfpd.cloud.common.config.datasource.MultiDataSourceNames;
import com.kfpd.cloud.common.showcase.CommerceRules;
import com.kfpd.cloud.manager.dao.cloud.DictionaryDao;
import com.kfpd.cloud.manager.pojo.vo.*;
import com.kfpd.cloud.manager.service.OperationLogService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.dao.DuplicateKeyException;

@Service
public class DictionaryService {
    private final DictionaryDao dao;
    private final OperationLogService logs;
    public DictionaryService(DictionaryDao dao,OperationLogService logs) { this.dao=dao;this.logs=logs; }
    private void kind(String kind) { CommerceRules.require(List.of("types","items").contains(kind),"字典资源不存在"); }
    public PageVO<Map<String,Object>> page(String kind,PageQueryVO input) {
        kind(kind);int page=Math.min(1000000,SystemManagementSupport.pageNum(input)),size=SystemManagementSupport.pageSize(input);
        Map<String,Object> q=new HashMap<>();q.put("kind",kind);q.put("keyword",SystemManagementSupport.keyword(input));q.put("status",SystemManagementSupport.status(input));q.put("pageSize",size);q.put("offset",(page-1)*size);
        return new PageVO<>(dao.count(q),page,size,dao.page(q));
    }
    public List<Map<String,Object>> types() { return dao.types(); }
    private String text(Map<String,Object> input,String key,int max,boolean required) {
        Object raw=input.get(key);
        String label=Map.of("typeCode","类型编码","itemCode","字典项编码","name","名称","description","说明","parentTypeCode","上级类型编码").get(key);
        CommerceRules.require(raw==null || raw instanceof String,label+"格式不正确");
        String value=raw==null ? "" : ((String)raw).trim();
        CommerceRules.require((!required || !value.isEmpty()) && value.length()<=max,"请填写"+label+"，最多"+max+"字");
        return value;
    }
    private String code(Map<String,Object> input,String key) {
        String value=text(input,key,64,true).toUpperCase(Locale.ROOT);
        CommerceRules.require(value.matches("[A-Z][A-Z0-9_]{0,63}"),"编码需以字母开头，仅含字母、数字和下划线，最多64位");return value;
    }
    @Transactional(transactionManager=MultiDataSourceNames.FA_CLOUD_TRANSACTION_MANAGER)
    public Map<String,Object> save(String kind,Long id,Map<String,Object> input) {
        kind(kind);CommerceRules.require(input!=null,"请填写字典资料");
        Map<String,Object> r=new HashMap<>();r.put("kind",kind);r.put("typeCode",code(input,"typeCode"));
        boolean merchant="MERCHANT_TYPE".equals(r.get("typeCode"));
        if("types".equals(kind)) {
            String parent=text(input,"parentTypeCode",64,false);
            r.put("parentTypeCode",parent.isEmpty() ? null : code(input,"parentTypeCode"));
            if(!parent.isEmpty()) {
                CommerceRules.require(!r.get("typeCode").equals(r.get("parentTypeCode")),"上级类型不能为自身");
                var parentType=dao.lockType((String)r.get("parentTypeCode"));
                CommerceRules.require(parentType!=null,"上级字典类型不存在");
                CommerceRules.require(parentType.get("parentTypeCode")==null,"仅支持大类和子类型两级字典类型");
                CommerceRules.require(dao.countItems((String)r.get("parentTypeCode"))==0,"上级类型已有字典项，不能作为大类");
            }
            merchant=merchant || "ECOMMERCE".equals(r.get("typeCode")) || "ECOMMERCE".equals(r.get("parentTypeCode"));
        }
        if("items".equals(kind)) {
            r.put("itemCode",code(input,"itemCode"));
            var type=dao.lockType((String)r.get("typeCode"));
            CommerceRules.require(type!=null,"字典类型不存在，请先创建字典类型");
            CommerceRules.require(!"ECOMMERCE".equals(r.get("typeCode")) && dao.countChildren((String)r.get("typeCode"))==0,"大类下请先创建子类型，再添加字典项");
            merchant=merchant || "ECOMMERCE".equals(type.get("parentTypeCode"));
            if(merchant) {
                dao.lockType("ECOMMERCE");
                CommerceRules.require(dao.conflictingMerchantItems(r)==0,"该商家类型编码已在其他电商分类中使用");
            }
        }
        r.put("name",text(input,"name",100,true));r.put("description",text(input,"description",500,false));
        if("items".equals(kind) && merchant)
            CommerceRules.require(((String)r.get("name")).length()<=32,"商家类型名称最多32字");
        r.put("status",CommerceRules.state(input.get("status"),List.of("ENABLED","DISABLED"),"ENABLED"));
        r.put("sortOrder",CommerceRules.integer(input.get("sortOrder"),0,Integer.MAX_VALUE));
        if(id!=null) {
            var before=dao.find(kind,id);if(before==null)throw SystemManagementSupport.notFound("字典记录不存在");
            CommerceRules.require(before.get("typeCode").equals(r.get("typeCode")),"字典类型编码创建后不可修改");
            if("items".equals(kind))CommerceRules.require(before.get("itemCode").equals(r.get("itemCode")),"字典项编码创建后不可修改");
            if("types".equals(kind)) CommerceRules.require(Objects.equals(before.get("parentTypeCode"),r.get("parentTypeCode")),"上级类型创建后不可修改");
            r.put("id",id);
        }
        try { if(id==null)dao.insert(r);else dao.update(r); }
        catch(DuplicateKeyException ex) { throw SystemManagementSupport.badRequest("编码已存在，同一类型下的字典项编码不能重复"); }
        if(merchant) { dao.syncMerchantTypes(); dao.refreshMerchantProducts(); }
        Long savedId=((Number)r.get("id")).longValue();
        if(id==null)logs.recordCreate("SYSTEM","DICTIONARY_"+kind.toUpperCase(Locale.ROOT),savedId,(String)r.get("name"),r);
        else logs.recordUpdate("SYSTEM","DICTIONARY_"+kind.toUpperCase(Locale.ROOT),id,(String)r.get("name"),"UPDATED",r);
        return r;
    }
    @Transactional(transactionManager=MultiDataSourceNames.FA_CLOUD_TRANSACTION_MANAGER)
    public void delete(String kind,Long id) {
        kind(kind);var record=dao.find(kind,id);if(record==null)throw SystemManagementSupport.notFound("字典记录不存在");
        if("types".equals(kind)) CommerceRules.require(dao.countChildren((String)record.get("typeCode"))==0,"大类下存在子类型，不能删除");
        if("types".equals(kind))CommerceRules.require(dao.countItems((String)record.get("typeCode"))==0,"类型下存在字典项，请先删除字典项或停用类型");
        var type="items".equals(kind) ? dao.lockType((String)record.get("typeCode")) : null;
        if("items".equals(kind) && ("MERCHANT_TYPE".equals(record.get("typeCode")) || (type!=null && "ECOMMERCE".equals(type.get("parentTypeCode"))))) {
            String code=(String)record.get("itemCode");
            dao.lockMerchantType(code);
            CommerceRules.require(dao.merchantReferences(code)==0,"商家或商品已引用此类型，请停用而非删除");
            dao.deleteMerchantType(code);
        }
        dao.delete(kind,id);logs.recordDelete("SYSTEM","DICTIONARY_"+kind.toUpperCase(Locale.ROOT),id,(String)record.get("name"),record);
    }
}
