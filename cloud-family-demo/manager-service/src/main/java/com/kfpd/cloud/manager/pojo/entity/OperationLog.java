package com.kfpd.cloud.manager.pojo.entity;

import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("operation_log")
public class OperationLog {

    @TableId(type = IdType.AUTO)
    private Long id;
    @TableField("operator_username")
    private String operatorUsername;
    @TableField("operator_user_type")
    private String operatorUserType;
    @TableField("operation_type")
    private String operationType;
    @TableField("business_module")
    private String businessModule;
    @TableField("business_type")
    private String businessType;
    @TableField("business_id")
    private String businessId;
    @TableField("business_name")
    private String businessName;
    @TableField("before_data")
    private String beforeData;
    @TableField("after_data")
    private String afterData;
    @TableField("client_ip")
    private String clientIp;
    @TableField("request_uri")
    private String requestUri;
    @TableField("request_method")
    private String requestMethod;
    @TableField("operation_at")
    private LocalDateTime operationAt;
    @TableField("created_at")
    private LocalDateTime createdAt;
}
