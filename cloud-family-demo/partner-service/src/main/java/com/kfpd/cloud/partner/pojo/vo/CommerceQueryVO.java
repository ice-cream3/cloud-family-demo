package com.kfpd.cloud.partner.pojo.vo;
public record CommerceQueryVO(Integer pageNum,Integer pageSize,String keyword,String category,String status,String typeCode) {
    public CommerceQueryVO(Integer pageNum,Integer pageSize,String keyword,String category,String status) { this(pageNum,pageSize,keyword,category,status,null); }
}
