package com.kfpd.cloud.manager.service.impl;

import java.time.LocalDateTime;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kfpd.cloud.common.web.GatewayHeaders;
import com.kfpd.cloud.manager.dao.OperationLogDao;
import com.kfpd.cloud.manager.pojo.entity.OperationLog;
import com.kfpd.cloud.manager.service.OperationLogService;
import jakarta.servlet.http.HttpServletRequest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Service
public class OperationLogServiceImpl implements OperationLogService {

    private static final Logger log = LoggerFactory.getLogger(OperationLogServiceImpl.class);
    private static final String OPERATION_CREATE = "CREATE";
    private static final String OPERATION_UPDATE = "UPDATE";
    private static final String OPERATION_DELETE = "DELETE";

    private final OperationLogDao operationLogDao;
    private final ObjectMapper objectMapper;

    public OperationLogServiceImpl(OperationLogDao operationLogDao, ObjectMapper objectMapper) {
        this.operationLogDao = operationLogDao;
        this.objectMapper = objectMapper;
    }

    @Override
    public void recordCreate(String businessModule, String businessType, Object businessId, String businessName, Object afterData) {
        record(OPERATION_CREATE, businessModule, businessType, businessId, businessName, null, afterData);
    }

    @Override
    public void recordUpdate(String businessModule, String businessType, Object businessId, String businessName, Object beforeData, Object afterData) {
        record(OPERATION_UPDATE, businessModule, businessType, businessId, businessName, beforeData, afterData);
    }

    @Override
    public void recordDelete(String businessModule, String businessType, Object businessId, String businessName, Object beforeData) {
        record(OPERATION_DELETE, businessModule, businessType, businessId, businessName, beforeData, null);
    }

    private void record(String operationType,
                        String businessModule,
                        String businessType,
                        Object businessId,
                        String businessName,
                        Object beforeData,
                        Object afterData) {
        try {
            HttpServletRequest request = currentRequest();
            OperationLog operationLog = new OperationLog();
            operationLog.setOperatorUsername(header(request, GatewayHeaders.USER_NAME, "anonymous"));
            operationLog.setOperatorUserType(header(request, GatewayHeaders.USER_TYPE, null));
            operationLog.setOperationType(operationType);
            operationLog.setBusinessModule(businessModule);
            operationLog.setBusinessType(businessType);
            operationLog.setBusinessId(businessId == null ? null : String.valueOf(businessId));
            operationLog.setBusinessName(businessName);
            operationLog.setBeforeData(toJson(beforeData));
            operationLog.setAfterData(toJson(afterData));
            operationLog.setClientIp(resolveClientIp(request));
            operationLog.setRequestUri(request == null ? null : request.getRequestURI());
            operationLog.setRequestMethod(request == null ? null : request.getMethod());
            operationLog.setOperationAt(LocalDateTime.now());
            operationLogDao.insert(operationLog);
        } catch (Exception ex) {
            log.error("Operation log write failed: operationType={}, businessModule={}, businessType={}, businessId={}, message={}",
                    operationType, businessModule, businessType, businessId, ex.getMessage(), ex);
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
