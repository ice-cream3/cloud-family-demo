package com.kfpd.cloud.common.exception;

import java.time.LocalDateTime;

public record ApiError(int code, String message, String path, LocalDateTime timestamp) {

    public static ApiError of(ErrorCode errorCode, String path) {
        return new ApiError(errorCode.getCode(), errorCode.getMessage(), path, LocalDateTime.now());
    }

    public static ApiError of(ErrorCode errorCode, String message, String path) {
        return new ApiError(errorCode.getCode(), message, path, LocalDateTime.now());
    }
}
