package com.kfpd.cloud.manager.service;

import com.kfpd.cloud.manager.pojo.entity.OperationLog;
import com.kfpd.cloud.manager.pojo.vo.PageQueryVO;
import com.kfpd.cloud.manager.pojo.vo.PageVO;

public interface OperationLogService {

    void recordCreate(String businessModule, String businessType, Object businessId, String businessName, Object afterData);

    void recordUpdate(String businessModule, String businessType, Object businessId, String businessName, Object beforeData, Object afterData);

    void recordDelete(String businessModule, String businessType, Object businessId, String businessName, Object beforeData);

    PageVO<OperationLog> findLogs(PageQueryVO query);
}
