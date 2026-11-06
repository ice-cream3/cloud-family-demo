package com.kfpd.cloud.partner.service;

import java.util.List;
import com.kfpd.cloud.common.showcase.CommerceTypePolicy;
import com.kfpd.cloud.common.config.datasource.MultiDataSourceNames;
import com.kfpd.cloud.partner.dao.cloud.CommerceTypeDao;
import org.springframework.transaction.annotation.Transactional;
import java.util.Map;
import com.kfpd.cloud.common.exception.BusinessException;
import com.kfpd.cloud.common.exception.ErrorCode;
import com.kfpd.cloud.common.showcase.CommerceRules;
import com.kfpd.cloud.partner.dao.cloud.CommerceDao;
import com.kfpd.cloud.partner.dao.cloud.VipUserDao;
import com.kfpd.cloud.partner.pojo.vo.CommerceQueryVO;
import com.kfpd.cloud.partner.pojo.vo.PageVO;
import org.springframework.stereotype.Service;

@Service
public class CommerceService {
    private final CommerceDao dao;
    private final VipUserDao users;
    private final CommerceTypeDao types;
    public CommerceService(CommerceDao dao, VipUserDao users,CommerceTypeDao types) { this.dao=dao; this.users=users;this.types=types; }
    public PageVO<Map<String,Object>> shops(CommerceQueryVO q) {
        var query=normalize(q); var rows=dao.shops(query.keyword(),shopTypeCodes(query),offset(query),query.pageSize()); CommerceTypePolicy.attach(types,rows); return new PageVO<>(dao.countShops(query.keyword(),shopTypeCodes(query)),query.pageNum(),query.pageSize(),rows);
    }
    public Map<String,Object> shop(Long id) { var shop=found(dao.shop(id));CommerceTypePolicy.attach(types,List.of(shop));return shop; }
    public PageVO<Map<String,Object>> products(Long shopId,CommerceQueryVO q) {
        shop(shopId); var query=normalize(q);
        return new PageVO<>(dao.countProducts(shopId,query.keyword(),query.typeCode()),query.pageNum(),query.pageSize(),dao.products(shopId,query.keyword(),query.typeCode(),offset(query),query.pageSize()));
    }
    public List<Map<String,Object>> categories(Long shopId) { shop(shopId); return dao.categories(shopId); }
    public Map<String,Object> product(Long id) { return found(dao.product(id)); }
    public Map<String,Object> mineShop(String username) { account(username); var shop=dao.mineShop(username);if(shop!=null)CommerceTypePolicy.attach(types,List.of(shop));return shop; }
    public List<Map<String,Object>> availableTypes() { return types.available(); }
    @Transactional(transactionManager=MultiDataSourceNames.FA_CLOUD_TRANSACTION_MANAGER)
    public Map<String,Object> saveShop(String username,Map<String,Object> input) {
        account(username);
        CommerceRules.require(input!=null,"请填写店铺资料");
        CommerceTypePolicy.codes(input.get("typeCodes"));
        var current=dao.mineShop(username);
        if(current!=null) {
            var locked=types.lockShop(((Number)current.get("id")).longValue());
            CommerceRules.require(locked!=null && "REJECTED".equals(locked.get("status")),"申请提交后仅可查看，审核通过后不能修改；驳回后可重新申请");
        }
        affected(dao.saveShop(username,CommerceRules.shop(input)));
        var saved=dao.mineShop(username);
        CommerceRules.require(saved!=null,"申请提交失败");
        long shopId=((Number)saved.get("id")).longValue();
        var locked=types.lockShop(shopId);
        CommerceRules.require(locked!=null && "PENDING".equals(locked.get("status")),"申请状态已变更，请刷新后重试");
        CommerceTypePolicy.assign(types,shopId,input.get("typeCodes"));
        return mineShop(username);
    }
    public PageVO<Map<String,Object>> mine(String username,CommerceQueryVO q) {
        account(username); var query=normalize(q);
        return new PageVO<>(dao.countMine(username,query.keyword(),query.status()),query.pageNum(),query.pageSize(),dao.mine(username,query.keyword(),query.status(),offset(query),query.pageSize()));
    }
    @Transactional(transactionManager=MultiDataSourceNames.FA_CLOUD_TRANSACTION_MANAGER)
    public void create(String username,Map<String,Object> input) {
        account(username);var shop=dao.mineShop(username);CommerceRules.require(shop!=null,"请先提交商家申请");CommerceRules.require("ACTIVE".equals(shop.get("status")),"商家需经后台审核通过且正常启用后才能添加商品");affected(dao.create(username,CommerceTypePolicy.product(types,((Number)shop.get("id")).longValue(),input)));
    }
    @Transactional(transactionManager=MultiDataSourceNames.FA_CLOUD_TRANSACTION_MANAGER)
    public void update(String username,Long id,Map<String,Object> input) {
        account(username);
        var owned=dao.publishState(username,id);
        if(owned==null) throw CommerceRules.bad("商品不存在或无权操作");
        var record=CommerceTypePolicy.product(types,((Number)owned.get("shopId")).longValue(),input);
        if ("PUBLISHED".equals(record.get("status"))) {
            var state=owned;
            if (state==null) throw CommerceRules.bad("商品不存在或无权操作");
            if (!"ACTIVE".equals(state.get("moderationStatus"))) throw CommerceRules.bad("商品已被后台停用，不能重新上架，请联系管理员");
            if (!"ACTIVE".equals(state.get("shopStatus"))) throw CommerceRules.bad("店铺已被后台停用，不能上架商品，请联系管理员");
        }
        // SQL repeats the state check to protect against concurrent moderation.
        affected(dao.update(username,id,record));
    }
    public void delete(String username,Long id) { account(username); affected(dao.delete(username,id)); }
    private void account(String username) {
        var user=users.findByUsername(username);
        if(user==null || !"ENABLED".equals(user.getStatus())) throw new BusinessException(ErrorCode.COMMON_FORBIDDEN,"账号不存在或已停用");
    }
    private void affected(int count) { if(count==0) throw new BusinessException(ErrorCode.COMMON_BAD_REQUEST,"记录不存在、无权操作或店铺/商品已被后台停用，请刷新后重试"); }
    private Map<String,Object> found(Map<String,Object> value) {
        if(value==null) throw new BusinessException(ErrorCode.COMMON_NOT_FOUND,"商家或商品不存在、已下架或已停用"); return value;
    }
    private int offset(CommerceQueryVO q) { return (q.pageNum()-1)*q.pageSize(); }
    private List<String> shopTypeCodes(CommerceQueryVO q) {
        var codes=new java.util.ArrayList<String>();
        if(q.typeCodes()!=null) {
            CommerceRules.require(q.typeCodes().size()<=100,"最多选择100个商家类型");
            for(String code:q.typeCodes()) codes.add(CommerceRules.typeCode(code));
        }
        if(q.typeCode()!=null) codes.add(q.typeCode());
        return codes.stream().distinct().toList();
    }
    private CommerceQueryVO normalize(CommerceQueryVO q) {
        if(q==null) return new CommerceQueryVO(1,12,null,null,null);
        CommerceRules.require(q.category()==null||q.category().isBlank(),"请使用typeCode筛选商品类型，名称仅用于展示");
        String status=q.status()==null||q.status().isBlank()?null:CommerceRules.state(q.status(),List.of("DRAFT","PUBLISHED","OFFLINE","SYSTEM_BLOCKED"),"DRAFT");
        return new CommerceQueryVO(q.pageNum()==null?1:Math.max(1,Math.min(1000000,q.pageNum())),q.pageSize()==null?12:Math.max(1,Math.min(100,q.pageSize())),trim(q.keyword(),100),null,status,q.typeCode()==null||q.typeCode().isBlank()?null:CommerceRules.typeCode(q.typeCode()),q.typeCodes());
    }
    private String trim(String value,int max) { if(value==null||value.isBlank()) return null; CommerceRules.require(value.trim().length()<=max,"查询条件过长"); return value.trim(); }
}
