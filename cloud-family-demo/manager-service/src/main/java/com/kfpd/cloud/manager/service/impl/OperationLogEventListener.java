package com.kfpd.cloud.manager.service.impl;

import java.time.LocalDateTime;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kfpd.cloud.manager.dao.model.OperationLogDao;
import com.kfpd.cloud.manager.pojo.entity.OperationLog;
import com.kfpd.cloud.manager.service.event.OperationLogEvent;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
public class OperationLogEventListener {

    private static final Logger log = LoggerFactory.getLogger(OperationLogEventListener.class);

    private final OperationLogDao operationLogDao;
    private final ObjectMapper objectMapper;

    public OperationLogEventListener(OperationLogDao operationLogDao,
                                     ObjectMapper objectMapper) {
        this.operationLogDao = operationLogDao;
        this.objectMapper = objectMapper;
    }

    @Async("managerOperationLogExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void onOperationLog(OperationLogEvent event) {
        try {
            OperationLog operationLog = new OperationLog();
            operationLog.setOperatorUsername(event.operatorUsername());
            operationLog.setOperatorUserType(event.operatorUserType());
            operationLog.setOperationType(event.operationType());
            operationLog.setBusinessModule(event.businessModule());
            operationLog.setBusinessType(event.businessType());
            operationLog.setBusinessId(event.businessId() == null ? null : String.valueOf(event.businessId()));
            operationLog.setBusinessName(event.businessName());
            operationLog.setBeforeData(toJson(event.beforeData()));
            operationLog.setAfterData(toJson(event.afterData()));
            operationLog.setClientIp(event.clientIp());
            operationLog.setRequestUri(event.requestUri());
            operationLog.setRequestMethod(event.requestMethod());
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

    private String toJson(Object value) {
        if (value == null) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException ex) {
            try {
                return objectMapper.writeValueAsString(String.valueOf(value));
            } catch (JsonProcessingException fallbackEx) {
                return "\"<unserializable operation log payload>\"";
            }
        }
    }
}
