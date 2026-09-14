package com.kfpd.cloud.manager.service.impl;

import java.time.LocalDateTime;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kfpd.cloud.common.web.GatewayHeaders;
import com.kfpd.cloud.manager.dao.OperationLogDao;
import com.kfpd.cloud.manager.pojo.entity.OperationLog;
import com.kfpd.cloud.manager.service.event.OperationLogEvent;
import jakarta.servlet.http.HttpServletRequest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Component
public class OperationLogEventListener {

    private static final Logger log = LoggerFactory.getLogger(OperationLogEventListener.class);

    private final OperationLogDao operationLogDao;
    private final ObjectMapper objectMapper;

    public OperationLogEventListener(OperationLogDao operationLogDao, ObjectMapper objectMapper) {
        this.operationLogDao = operationLogDao;
        this.objectMapper = objectMapper;
    }

    @Async("managerOperationLogExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void onOperationLog(OperationLogEvent event) {
        try {
            HttpServletRequest request = currentRequest();
            OperationLog operationLog = new OperationLog();
            operationLog.setOperatorUsername(header(request, GatewayHeaders.USER_NAME, "anonymous"));
            operationLog.setOperatorUserType(header(request, GatewayHeaders.USER_TYPE, null));
            operationLog.setOperationType(event.operationType());
            operationLog.setBusinessModule(event.businessModule());
            operationLog.setBusinessType(event.businessType());
            operationLog.setBusinessId(event.businessId() == null ? null : String.valueOf(event.businessId()));
            operationLog.setBusinessName(event.businessName());
            operationLog.setBeforeData(toJson(event.beforeData()));
            operationLog.setAfterData(toJson(event.afterData()));
            operationLog.setClientIp(resolveClientIp(request));
            operationLog.setRequestUri(request == null ? null : request.getRequestURI());
            operationLog.setRequestMethod(request == null ? null : request.getMethod());
            operationLog.setOperationAt(LocalDateTime.now());
            operationLogDao.insert(operationLog);
        } catch (Exception ex) {
            log.error("Operation log listener failed: operationType={}, businessModule={}, businessType={}, businessId={}, message={}",
                    event.operationType(),
                    event.businessModule(),
                    event.businessType(),
                    event.businessId(),
                    ex.getMessage(),
                    ex);
        }
    }

    private HttpServletRequest currentRequest() {
        if (RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attributes) {
            return attributes.getRequest();
        }
        return null;
    }

    private String header(HttpServletRequest request, String name, String defaultValue) {
        if (request == null) {
            return defaultValue;
        }
        String value = request.getHeader(name);
        return value == null || value.isBlank() ? defaultValue : value;
    }

    private String resolveClientIp(HttpServletRequest request) {
        if (request == null) {
            return null;
        }
        String forwardedFor = request.getHeader("X-Forwarded-For");
        if (forwardedFor != null && !forwardedFor.isBlank()) {
            return forwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    private String toJson(Object value) {
        if (value == null) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException ex) {
            return String.valueOf(value);
        }
    }
}
