package com.kfpd.cloud.manager.service.impl;

import com.kfpd.cloud.manager.service.OperationLogService;
import com.kfpd.cloud.manager.service.event.OperationLogEvent;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

@Service
public class OperationLogServiceImpl implements OperationLogService {

    private static final String OPERATION_CREATE = "CREATE";
    private static final String OPERATION_UPDATE = "UPDATE";
    private static final String OPERATION_DELETE = "DELETE";

    private final ApplicationEventPublisher eventPublisher;

    public OperationLogServiceImpl(ApplicationEventPublisher eventPublisher) {
        this.eventPublisher = eventPublisher;
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
        eventPublisher.publishEvent(new OperationLogEvent(
                operationType,
                businessModule,
                businessType,
                businessId,
                businessName,
                beforeData,
                afterData
        ));
    }

}
