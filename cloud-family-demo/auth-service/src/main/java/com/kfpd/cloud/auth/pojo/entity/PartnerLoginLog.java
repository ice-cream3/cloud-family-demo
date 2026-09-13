package com.kfpd.cloud.auth.pojo.entity;

import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("partner_login_log")
public class PartnerLoginLog {

    @TableId(type = IdType.AUTO)
    private Long id;
    private String username;
    private String userType;
    private String clientId;
    private String grantType;
    private String clientIp;
    private String userAgent;
    private String deviceType;
    private String browser;
    private String operatingSystem;
    private LocalDateTime loginAt;
    private String loginResult;
    private String failureReason;
    private LocalDateTime createdAt;
}
