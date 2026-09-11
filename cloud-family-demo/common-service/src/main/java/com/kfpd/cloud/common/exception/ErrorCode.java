package com.kfpd.cloud.common.exception;

public enum ErrorCode {

    COMMON_BAD_REQUEST(40000, 400, "Bad request"),
    COMMON_UNAUTHORIZED(40100, 401, "Unauthorized"),
    COMMON_FORBIDDEN(40300, 403, "Forbidden"),
    COMMON_NOT_FOUND(40400, 404, "Resource not found"),
    COMMON_INTERNAL_ERROR(50000, 500, "Internal server error"),

    AUTH_INVALID_API_CREDENTIALS(100001, 401, "Invalid api username or password"),
    AUTH_MANAGER_IP_FORBIDDEN(100002, 403, "Manager login IP is not allowed"),
    AUTH_INVALID_MANAGER_CREDENTIALS(100003, 401, "Invalid manager username or password"),
    AUTH_MANAGER_PERMISSION_REQUIRED(100004, 403, "Manager login permission required"),
    AUTH_CLIENT_NOT_INITIALIZED(100005, 500, "OAuth2 registered client is not initialized"),
    AUTH_INVALID_REFRESH_TOKEN(100006, 401, "Invalid refresh token"),
    AUTH_REGISTERED_CLIENT_NOT_FOUND(100007, 401, "Registered client not found"),
    AUTHORIZATION_METADATA_INCOMPLETE(100008, 401, "Authorization metadata is incomplete"),
    AUTH_TOO_MANY_API_LOGIN_ATTEMPTS(100009, 429, "Too many api login attempts"),

    PARTNER_VIP_USER_NOT_FOUND(200001, 404, "Vip user not found"),

    MANAGER_RESOURCE_NOT_FOUND(300001, 404, "Manager resource not found"),

    GATEWAY_MISSING_AUTHENTICATED_JWT(400001, 401, "Missing authenticated JWT"),
    GATEWAY_MANAGER_ROLE_REQUIRED(400002, 403, "Manager role required");

    private final int code;
    private final int httpStatus;
    private final String message;

    ErrorCode(int code, int httpStatus, String message) {
        this.code = code;
        this.httpStatus = httpStatus;
        this.message = message;
    }

    public int getCode() {
        return code;
    }

    public int getHttpStatus() {
        return httpStatus;
    }

    public String getMessage() {
        return message;
    }
}
