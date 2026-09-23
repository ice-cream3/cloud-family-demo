package com.kfpd.cloud.manager.pojo.vo;

import lombok.Data;

@Data
public class PageQueryVO {

    private Integer pageNum;
    private Integer pageSize;
    private String keyword;
    private String status;
    private String operatorUsername;
    private String businessName;
    private String businessModule;
    private String businessType;

    public Integer pageNum() {
        return pageNum;
    }

    public Integer pageSize() {
        return pageSize;
    }

    public String keyword() {
        return keyword;
    }

    public String status() {
        return status;
    }

    public String operatorUsername() {
        return operatorUsername;
    }

    public String businessName() {
        return businessName;
    }

    public String businessModule() {
        return businessModule;
    }

    public String businessType() {
        return businessType;
    }
}
