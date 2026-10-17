package com.kfpd.cloud.auth.pojo.entity;

import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * 管理端登录日志，对应 fa-model.manager_login_log 表。
 */
@Data
@TableName("manager_login_log")
public class ManagerLoginLog {

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
    private String userType;

    /**
     * OAuth2 客户端标识。
     */
    private String clientId;

    /**
     * 登录使用的授权类型。
     */
    private String grantType;

    /**
     * 客户端 IP 地址。
     */
    private String clientIp;

    /**
     * 客户端 User-Agent 请求头。
     */
    private String userAgent;

    /**
     * 客户端设备类型。
     */
    private String deviceType;

    /**
     * 浏览器名称。
     */
    private String browser;

    /**
     * 客户端操作系统。
     */
    private String operatingSystem;

    /**
     * 登录时间。
     */
    private LocalDateTime loginAt;

    /**
     * 登录结果：SUCCESS 成功、FAILURE 失败。
     */
    private String loginResult;

    /**
     * 登录失败原因，登录成功时可为空。
     */
    private String failureReason;

    /**
     * 创建时间。
     */
    private LocalDateTime createdAt;
}
