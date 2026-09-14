package com.kfpd.cloud.manager.service.event;

public record OperationLogEvent(
        String operationType,
        String businessModule,
        String businessType,
        Object businessId,
        String businessName,
        Object beforeData,
        Object afterData
) {
}
