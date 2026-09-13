package com.kfpd.cloud.manager.pojo.entity;

import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("operation_log")
public class OperationLog {

    @TableId(type = IdType.AUTO)
    private Long id;
    private String operatorUsername;
    private String operatorUserType;
    private String operationType;
    private String businessModule;
    private String businessType;
    private String businessId;
    private String businessName;
    private String beforeData;
    private String afterData;
    private String clientIp;
    private String requestUri;
    private String requestMethod;
    private LocalDateTime operationAt;
    private LocalDateTime createdAt;
}
