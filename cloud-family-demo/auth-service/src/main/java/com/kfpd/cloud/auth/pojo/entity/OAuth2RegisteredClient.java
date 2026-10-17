package com.kfpd.cloud.auth.pojo.entity;

import java.time.Instant;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * OAuth2 注册客户端，对应 fa-cloud.oauth2_registered_client 表。
 */
@Data
@TableName("oauth2_registered_client")
public class OAuth2RegisteredClient {

    /**
     * 注册客户端记录 ID，由应用指定。
     */
    @TableId(value = "id", type = IdType.INPUT)
    private String id;

    /**
     * OAuth2 客户端标识。
     */
    @TableField("client_id")
    private String clientId;

    /**
     * 客户端标识签发时间。
     */
    @TableField("client_id_issued_at")
    private Instant clientIdIssuedAt;

    /**
     * 客户端密钥存储值。
     */
    @TableField("client_secret")
    private String clientSecret;

    /**
     * 客户端密钥到期时间，为空表示不设置到期时间。
     */
    @TableField("client_secret_expires_at")
    private Instant clientSecretExpiresAt;

    /**
     * 客户端显示名称。
     */
    @TableField("client_name")
    private String clientName;

    /**
     * 客户端认证方式，多个值以逗号分隔。
     */
    @TableField("client_authentication_methods")
    private String clientAuthenticationMethods;

    /**
     * 允许的授权类型，多个值以逗号分隔。
     */
    @TableField("authorization_grant_types")
    private String authorizationGrantTypes;

    /**
     * 授权回调地址，多个值以逗号分隔。
     */
    @TableField("redirect_uris")
    private String redirectUris;

    /**
     * 退出登录后的回调地址，多个值以逗号分隔。
     */
    @TableField("post_logout_redirect_uris")
    private String postLogoutRedirectUris;

    /**
     * 允许的授权范围，多个值以逗号分隔。
     */
    @TableField("scopes")
    private String scopes;

    /**
     * 客户端配置，JSON 字符串。
     */
    @TableField("client_settings")
    private String clientSettings;

    /**
     * 令牌配置，JSON 字符串。
     */
    @TableField("token_settings")
    private String tokenSettings;
}
