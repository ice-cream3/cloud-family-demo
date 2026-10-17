package com.kfpd.cloud.auth.pojo.entity;

import java.time.Instant;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * OAuth2 授权记录，对应 fa-cloud.oauth2_authorization 表。
 */
@Data
@TableName("oauth2_authorization")
public class OAuth2Authorization {

    /**
     * 授权记录 ID，由授权服务生成。
     */
    @TableId(value = "id", type = IdType.INPUT)
    private String id;

    /**
     * 注册客户端记录 ID（oauth2_registered_client.id）。
     */
    @TableField("registered_client_id")
    private String registeredClientId;

    /**
     * 授权主体的用户账号。
     */
    @TableField("principal_name")
    private String principalName;

    /**
     * 本次授权使用的授权类型。
     */
    @TableField("authorization_grant_type")
    private String authorizationGrantType;

    /**
     * 实际授予的权限范围，多个值以逗号分隔。
     */
    @TableField("authorized_scopes")
    private String authorizedScopes;

    /**
     * 授权附加属性的序列化二进制数据。
     */
    @TableField("attributes")
    private byte[] attributes;

    /**
     * OAuth2 授权请求的状态值。
     */
    @TableField("state")
    private String state;

    /**
     * 授权码的二进制存储值。
     */
    @TableField("authorization_code_value")
    private byte[] authorizationCodeValue;

    /**
     * 授权码签发时间。
     */
    @TableField("authorization_code_issued_at")
    private Instant authorizationCodeIssuedAt;

    /**
     * 授权码到期时间。
     */
    @TableField("authorization_code_expires_at")
    private Instant authorizationCodeExpiresAt;

    /**
     * 授权码元数据的序列化二进制数据。
     */
    @TableField("authorization_code_metadata")
    private byte[] authorizationCodeMetadata;

    /**
     * 访问令牌的二进制存储值。
     */
    @TableField("access_token_value")
    private byte[] accessTokenValue;

    /**
     * 访问令牌签发时间。
     */
    @TableField("access_token_issued_at")
    private Instant accessTokenIssuedAt;

    /**
     * 访问令牌到期时间。
     */
    @TableField("access_token_expires_at")
    private Instant accessTokenExpiresAt;

    /**
     * 访问令牌元数据的序列化二进制数据。
     */
    @TableField("access_token_metadata")
    private byte[] accessTokenMetadata;

    /**
     * 访问令牌类型，例如 Bearer。
     */
    @TableField("access_token_type")
    private String accessTokenType;

    /**
     * 访问令牌权限范围，多个值以逗号分隔。
     */
    @TableField("access_token_scopes")
    private String accessTokenScopes;

    /**
     * OIDC ID 令牌的二进制存储值。
     */
    @TableField("oidc_id_token_value")
    private byte[] oidcIdTokenValue;

    /**
     * OIDC ID 令牌签发时间。
     */
    @TableField("oidc_id_token_issued_at")
    private Instant oidcIdTokenIssuedAt;

    /**
     * OIDC ID 令牌到期时间。
     */
    @TableField("oidc_id_token_expires_at")
    private Instant oidcIdTokenExpiresAt;

    /**
     * OIDC ID 令牌元数据的序列化二进制数据。
     */
    @TableField("oidc_id_token_metadata")
    private byte[] oidcIdTokenMetadata;

    /**
     * 刷新令牌的二进制存储值。
     */
    @TableField("refresh_token_value")
    private byte[] refreshTokenValue;

    /**
     * 刷新令牌签发时间。
     */
    @TableField("refresh_token_issued_at")
    private Instant refreshTokenIssuedAt;

    /**
     * 刷新令牌到期时间。
     */
    @TableField("refresh_token_expires_at")
    private Instant refreshTokenExpiresAt;

    /**
     * 刷新令牌元数据的序列化二进制数据。
     */
    @TableField("refresh_token_metadata")
    private byte[] refreshTokenMetadata;

    /**
     * 设备授权用户码的二进制存储值。
     */
    @TableField("user_code_value")
    private byte[] userCodeValue;

    /**
     * 设备授权用户码签发时间。
     */
    @TableField("user_code_issued_at")
    private Instant userCodeIssuedAt;

    /**
     * 设备授权用户码到期时间。
     */
    @TableField("user_code_expires_at")
    private Instant userCodeExpiresAt;

    /**
     * 设备授权用户码元数据的序列化二进制数据。
     */
    @TableField("user_code_metadata")
    private byte[] userCodeMetadata;

    /**
     * 设备授权设备码的二进制存储值。
     */
    @TableField("device_code_value")
    private byte[] deviceCodeValue;

    /**
     * 设备授权设备码签发时间。
     */
    @TableField("device_code_issued_at")
    private Instant deviceCodeIssuedAt;

    /**
     * 设备授权设备码到期时间。
     */
    @TableField("device_code_expires_at")
    private Instant deviceCodeExpiresAt;

    /**
     * 设备授权设备码元数据的序列化二进制数据。
     */
    @TableField("device_code_metadata")
    private byte[] deviceCodeMetadata;
}
