package com.kfpd.cloud.partner.base.config;

import com.kfpd.cloud.common.exception.ApiError;
import com.kfpd.cloud.common.exception.BusinessException;
import com.kfpd.cloud.common.exception.ErrorCode;
import jakarta.servlet.http.HttpServletRequest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.server.ResponseStatusException;

@RestControllerAdvice
public class PartnerExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(PartnerExceptionHandler.class);

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiError> handleBusinessException(BusinessException ex, HttpServletRequest request) {
        log.warn("Business exception: code={}, status={}, method={}, path={}, message={}",
                ex.getCode(), ex.getHttpStatus(), request.getMethod(), request.getRequestURI(), ex.getMessage());
        return ResponseEntity.status(ex.getHttpStatus()).body(ApiError.of(ex.getErrorCode(), ex.getMessage(), request.getRequestURI()));
    }

    @ExceptionHandler({
            MethodArgumentNotValidException.class,
            MissingServletRequestParameterException.class,
            MethodArgumentTypeMismatchException.class
    })
    public ResponseEntity<ApiError> handleBadRequest(Exception ex, HttpServletRequest request) {
        log.warn("Bad request: code={}, method={}, path={}, message={}",
                ErrorCode.COMMON_BAD_REQUEST.getCode(), request.getMethod(), request.getRequestURI(), ex.getMessage());
        return ResponseEntity.badRequest().body(ApiError.of(ErrorCode.COMMON_BAD_REQUEST, ex.getMessage(), request.getRequestURI()));
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ApiError> handleResponseStatusException(ResponseStatusException ex, HttpServletRequest request) {
        HttpStatus status = HttpStatus.resolve(ex.getStatusCode().value());
        ErrorCode errorCode = switch (ex.getStatusCode().value()) {
            case 401 -> ErrorCode.COMMON_UNAUTHORIZED;
            case 403 -> ErrorCode.COMMON_FORBIDDEN;
            case 404 -> ErrorCode.COMMON_NOT_FOUND;
            default -> ErrorCode.COMMON_INTERNAL_ERROR;
        };
        log.warn("Response status exception: code={}, status={}, method={}, path={}, message={}",
                errorCode.getCode(), ex.getStatusCode().value(), request.getMethod(), request.getRequestURI(), ex.getReason());
        return ResponseEntity.status(status == null ? HttpStatus.INTERNAL_SERVER_ERROR : status)
                .body(ApiError.of(errorCode, ex.getReason(), request.getRequestURI()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> handleException(Exception ex, HttpServletRequest request) {
        log.error("Unhandled exception: code={}, method={}, path={}",
                ErrorCode.COMMON_INTERNAL_ERROR.getCode(), request.getMethod(), request.getRequestURI(), ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiError.of(ErrorCode.COMMON_INTERNAL_ERROR, request.getRequestURI()));
    }
}
