package com.kfpd.cloud.partner.service;

import java.util.List;
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
    public CommerceService(CommerceDao dao, VipUserDao users) { this.dao=dao; this.users=users; }
    public PageVO<Map<String,Object>> shops(CommerceQueryVO q) {
        var query=normalize(q); return new PageVO<>(dao.countShops(query.keyword()),query.pageNum(),query.pageSize(),dao.shops(query.keyword(),offset(query),query.pageSize()));
    }
    public Map<String,Object> shop(Long id) { return found(dao.shop(id)); }
    public PageVO<Map<String,Object>> products(Long shopId,CommerceQueryVO q) {
        shop(shopId); var query=normalize(q);
        return new PageVO<>(dao.countProducts(shopId,query.keyword(),query.category()),query.pageNum(),query.pageSize(),dao.products(shopId,query.keyword(),query.category(),offset(query),query.pageSize()));
    }
    public List<String> categories(Long shopId) { shop(shopId); return dao.categories(shopId); }
    public Map<String,Object> product(Long id) { return found(dao.product(id)); }
    public Map<String,Object> mineShop(String username) { account(username); return dao.mineShop(username); }
    public Map<String,Object> saveShop(String username,Map<String,Object> input) {
        account(username); dao.saveShop(username,CommerceRules.shop(input)); return dao.mineShop(username);
    }
    public PageVO<Map<String,Object>> mine(String username,CommerceQueryVO q) {
        account(username); var query=normalize(q);
        return new PageVO<>(dao.countMine(username,query.keyword(),query.status()),query.pageNum(),query.pageSize(),dao.mine(username,query.keyword(),query.status(),offset(query),query.pageSize()));
    }
    public void create(String username,Map<String,Object> input) {
        account(username); affected(dao.create(username,CommerceRules.product(input)));
    }
    public void update(String username,Long id,Map<String,Object> input) {
        account(username);
        var record=CommerceRules.product(input);
        if ("PUBLISHED".equals(record.get("status"))) {
            var state=dao.publishState(username,id);
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
    private CommerceQueryVO normalize(CommerceQueryVO q) {
        if(q==null) return new CommerceQueryVO(1,12,null,null,null);
        String status=q.status()==null||q.status().isBlank()?null:CommerceRules.state(q.status(),List.of("DRAFT","PUBLISHED","OFFLINE","SYSTEM_BLOCKED"),"DRAFT");
        return new CommerceQueryVO(q.pageNum()==null?1:Math.max(1,Math.min(1000000,q.pageNum())),q.pageSize()==null?12:Math.max(1,Math.min(100,q.pageSize())),trim(q.keyword(),100),trim(q.category(),32),status);
    }
    private String trim(String value,int max) { if(value==null||value.isBlank()) return null; CommerceRules.require(value.trim().length()<=max,"查询条件过长"); return value.trim(); }
}
