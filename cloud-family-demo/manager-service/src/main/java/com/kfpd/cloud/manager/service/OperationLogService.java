package com.kfpd.cloud.manager.service;

public interface OperationLogService {

    void recordCreate(String businessModule, String businessType, Object businessId, String businessName, Object afterData);

    void recordUpdate(String businessModule, String businessType, Object businessId, String businessName, Object beforeData, Object afterData);

    void recordDelete(String businessModule, String businessType, Object businessId, String businessName, Object beforeData);
}
