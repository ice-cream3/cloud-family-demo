package com.kfpd.cloud.manager.pojo.entity;

import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * 业务操作日志，对应 fa-model.operation_log 表。
 */
@Data
@TableName("operation_log")
public class OperationLog {

    /**
     * 主键 ID。
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 操作人账号。
     */
    @TableField("operator_username")
    private String operatorUsername;

    /**
     * 操作人用户类型。
     */
    @TableField("operator_user_type")
    private String operatorUserType;

    /**
     * 操作类型，例如新增、修改、删除。
     */
    @TableField("operation_type")
    private String operationType;

    /**
     * 业务模块标识。
     */
    @TableField("business_module")
    private String businessModule;

    /**
     * 业务对象类型。
     */
    @TableField("business_type")
    private String businessType;

    /**
     * 业务记录主键或标识。
     */
    @TableField("business_id")
    private String businessId;

    /**
     * 业务记录名称。
     */
    @TableField("business_name")
    private String businessName;

    /**
     * 操作前的业务数据，JSON 字符串。
     */
    @TableField("before_data")
    private String beforeData;

    /**
     * 操作后的业务数据，JSON 字符串。
     */
    @TableField("after_data")
    private String afterData;

    /**
     * 客户端 IP 地址。
     */
    @TableField("client_ip")
    private String clientIp;

    /**
     * 请求 URI。
     */
    @TableField("request_uri")
    private String requestUri;

    /**
     * HTTP 请求方法。
     */
    @TableField("request_method")
    private String requestMethod;

    /**
     * 业务操作发生时间。
     */
    @TableField("operation_at")
    private LocalDateTime operationAt;

    /**
     * 创建时间。
     */
    @TableField("created_at")
    private LocalDateTime createdAt;
}
