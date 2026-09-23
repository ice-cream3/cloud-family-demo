package com.kfpd.cloud.manager.pojo.entity;

import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("partner_login_log")
public class PartnerLoginLog {

    @TableId(type = IdType.AUTO)
    private Long id;
    private String username;
    @TableField("user_type")
    private String userType;
    @TableField("client_id")
    private String clientId;
    @TableField("grant_type")
    private String grantType;
    @TableField("client_ip")
    private String clientIp;
    @TableField("user_agent")
    private String userAgent;
    @TableField("device_type")
    private String deviceType;
    private String browser;
    @TableField("operating_system")
    private String operatingSystem;
    @TableField("login_at")
    private LocalDateTime loginAt;
    @TableField("login_result")
    private String loginResult;
    @TableField("failure_reason")
    private String failureReason;
    @TableField("created_at")
    private LocalDateTime createdAt;
}
