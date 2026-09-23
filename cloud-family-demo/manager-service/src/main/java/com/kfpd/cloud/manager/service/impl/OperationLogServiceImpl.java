package com.kfpd.cloud.manager.service.impl;

import com.alibaba.fastjson2.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.kfpd.cloud.common.web.GatewayHeaders;
import com.kfpd.cloud.manager.dao.model.OperationLogDao;
import com.kfpd.cloud.manager.pojo.entity.OperationLog;
import com.kfpd.cloud.manager.pojo.vo.PageQueryVO;
import com.kfpd.cloud.manager.pojo.vo.PageVO;
import com.kfpd.cloud.manager.service.OperationLogService;
import com.kfpd.cloud.manager.service.event.OperationLogEvent;
import jakarta.servlet.http.HttpServletRequest;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Service
public class OperationLogServiceImpl implements OperationLogService {

    private static final String OPERATION_CREATE = "CREATE";
    private static final String OPERATION_UPDATE = "UPDATE";
    private static final String OPERATION_DELETE = "DELETE";

    private final ApplicationEventPublisher eventPublisher;
    private final OperationLogDao operationLogDao;

    public OperationLogServiceImpl(ApplicationEventPublisher eventPublisher,
                                   OperationLogDao operationLogDao) {
        this.eventPublisher = eventPublisher;
        this.operationLogDao = operationLogDao;
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

    @Override
    public PageVO<OperationLog> findLogs(PageQueryVO query) {
        int pageNum = SystemManagementSupport.pageNum(query);
        int pageSize = SystemManagementSupport.pageSize(query);
        String keyword = SystemManagementSupport.keyword(query);
        String operationType = SystemManagementSupport.status(query);
        String operatorUsername = normalize(query.operatorUsername());
        String businessName = normalize(query.businessName());
        String businessModule = normalize(query.businessModule());
        String businessType = normalize(query.businessType());
        LambdaQueryWrapper<OperationLog> wrapper = Wrappers.lambdaQuery(OperationLog.class)
                .eq(SystemManagementSupport.hasText(operationType), OperationLog::getOperationType, operationType)
                .like(SystemManagementSupport.hasText(operatorUsername), OperationLog::getOperatorUsername, operatorUsername)
                .like(SystemManagementSupport.hasText(businessName), OperationLog::getBusinessName, businessName)
                .eq(SystemManagementSupport.hasText(businessModule), OperationLog::getBusinessModule, businessModule)
                .eq(SystemManagementSupport.hasText(businessType), OperationLog::getBusinessType, businessType)
                .and(SystemManagementSupport.hasText(keyword), condition -> condition
                        .like(OperationLog::getOperatorUsername, keyword)
                        .or()
                        .like(OperationLog::getBusinessModule, keyword)
                        .or()
                        .like(OperationLog::getBusinessType, keyword)
                        .or()
                        .like(OperationLog::getBusinessId, keyword)
                        .or()
                        .like(OperationLog::getBusinessName, keyword)
                        .or()
                        .like(OperationLog::getRequestUri, keyword)
                )
                .orderByDesc(OperationLog::getOperationAt)
                .orderByDesc(OperationLog::getId);
        Page<OperationLog> page = operationLogDao.selectPage(
                new Page<>(pageNum, pageSize),
                wrapper
        );
        return SystemManagementSupport.pageVO(page, pageNum, pageSize);
    }

    private String normalize(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private void record(String operationType,
                        String businessModule,
                        String businessType,
                        Object businessId,
                        String businessName,
                        Object beforeData,
                        Object afterData) {
        HttpServletRequest request = currentRequest();
        eventPublisher.publishEvent(new OperationLogEvent(
                operationType,
                businessModule,
                businessType,
                businessId,
                businessName,
                JSONObject.toJSONString(beforeData),
                JSONObject.toJSONString(afterData),
                header(request, GatewayHeaders.USER_NAME, "anonymous"),
                header(request, GatewayHeaders.USER_TYPE, null),
                resolveClientIp(request),
                request == null ? null : request.getRequestURI(),
                request == null ? null : request.getMethod()
        ));
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

}
