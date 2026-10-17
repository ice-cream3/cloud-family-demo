package com.kfpd.cloud.manager.pojo.entity;

import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * 用户端登录日志，对应 fa-model.partner_login_log 表。
 */
@Data
@TableName("partner_login_log")
public class PartnerLoginLog {

    /**
     * 主键 ID。
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 用户账号。
     */
    private String username;

    /**
     * 用户类型，例如 API、MANAGER。
     */
    @TableField("user_type")
    private String userType;

    /**
     * OAuth2 客户端标识。
     */
    @TableField("client_id")
    private String clientId;

    /**
     * 登录使用的授权类型。
     */
    @TableField("grant_type")
    private String grantType;

    /**
     * 客户端 IP 地址。
     */
    @TableField("client_ip")
    private String clientIp;

    /**
     * 客户端 User-Agent 请求头。
     */
    @TableField("user_agent")
    private String userAgent;

    /**
     * 客户端设备类型。
     */
    @TableField("device_type")
    private String deviceType;

    /**
     * 浏览器名称。
     */
    private String browser;

    /**
     * 客户端操作系统。
     */
    @TableField("operating_system")
    private String operatingSystem;

    /**
     * 登录时间。
     */
    @TableField("login_at")
    private LocalDateTime loginAt;

    /**
     * 登录结果：SUCCESS 成功、FAILURE 失败。
     */
    @TableField("login_result")
    private String loginResult;

    /**
     * 登录失败原因，登录成功时可为空。
     */
    @TableField("failure_reason")
    private String failureReason;

    /**
     * 创建时间。
     */
    @TableField("created_at")
    private LocalDateTime createdAt;
}
