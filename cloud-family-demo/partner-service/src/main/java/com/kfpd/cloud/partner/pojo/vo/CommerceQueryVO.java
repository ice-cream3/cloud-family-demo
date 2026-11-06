package com.kfpd.cloud.partner.pojo.vo;
import java.util.List;
public record CommerceQueryVO(Integer pageNum,Integer pageSize,String keyword,String category,String status,String typeCode,List<String> typeCodes) {
    public CommerceQueryVO(Integer pageNum,Integer pageSize,String keyword,String category,String status,String typeCode) { this(pageNum,pageSize,keyword,category,status,typeCode,null); }
    public CommerceQueryVO(Integer pageNum,Integer pageSize,String keyword,String category,String status) { this(pageNum,pageSize,keyword,category,status,null,null); }
}
