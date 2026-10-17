package com.kfpd.cloud.auth.pojo.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * OAuth2 用户授权同意记录，对应 fa-cloud.oauth2_authorization_consent 表。
 * 联合主键：registered_client_id、principal_name；读写时需同时指定全部主键字段。
 */
@Data
@TableName("oauth2_authorization_consent")
public class OAuth2AuthorizationConsent {

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
     * 用户同意授予的权限，多个值以逗号分隔。
     */
    @TableField("authorities")
    private String authorities;
}
