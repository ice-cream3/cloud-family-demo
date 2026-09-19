package com.kfpd.cloud.manager.service.impl;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

import com.kfpd.cloud.common.config.datasource.MultiDataSourceNames;
import com.kfpd.cloud.common.web.GatewayHeaders;
import com.kfpd.cloud.manager.pojo.entity.OperationLog;
import com.kfpd.cloud.manager.pojo.vo.PageQueryVO;
import com.kfpd.cloud.manager.pojo.vo.PageVO;
import com.kfpd.cloud.manager.service.OperationLogService;
import com.kfpd.cloud.manager.service.event.OperationLogEvent;
import jakarta.servlet.http.HttpServletRequest;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Service
public class OperationLogServiceImpl implements OperationLogService {

    private static final String OPERATION_CREATE = "CREATE";
    private static final String OPERATION_UPDATE = "UPDATE";
    private static final String OPERATION_DELETE = "DELETE";

    private final ApplicationEventPublisher eventPublisher;
    private final JdbcTemplate faModelJdbcTemplate;

    public OperationLogServiceImpl(ApplicationEventPublisher eventPublisher,
                                   @Qualifier(MultiDataSourceNames.FA_MODEL_JDBC_TEMPLATE) JdbcTemplate faModelJdbcTemplate) {
        this.eventPublisher = eventPublisher;
        this.faModelJdbcTemplate = faModelJdbcTemplate;
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
        List<Object> args = new ArrayList<>();
        String where = buildWhere(keyword, operationType, args);
        Long total = faModelJdbcTemplate.queryForObject("SELECT COUNT(*) FROM operation_log" + where, Long.class, args.toArray());
        List<Object> pageArgs = new ArrayList<>(args);
        pageArgs.add((pageNum - 1) * pageSize);
        pageArgs.add(pageSize);
        List<OperationLog> records = faModelJdbcTemplate.query(
                "SELECT id, operator_username, operator_user_type, operation_type, business_module, "
                        + "business_type, business_id, business_name, before_data, after_data, client_ip, "
                        + "request_uri, request_method, operation_at, created_at "
                        + "FROM operation_log" + where + " ORDER BY operation_at DESC, id DESC LIMIT ?, ?",
                operationLogRowMapper(),
                pageArgs.toArray()
        );
        return new PageVO<>(total == null ? 0 : total, pageNum, pageSize, records);
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
                beforeData,
                afterData,
                header(request, GatewayHeaders.USER_NAME, "anonymous"),
                header(request, GatewayHeaders.USER_TYPE, null),
                resolveClientIp(request),
                request == null ? null : request.getRequestURI(),
                request == null ? null : request.getMethod()
        ));
    }

    private String buildWhere(String keyword, String operationType, List<Object> args) {
        StringBuilder where = new StringBuilder(" WHERE 1 = 1");
        if (SystemManagementSupport.hasText(operationType)) {
            where.append(" AND operation_type = ?");
            args.add(operationType);
        }
        if (SystemManagementSupport.hasText(keyword)) {
            String like = "%" + keyword.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_") + "%";
            where.append("""
                     AND (
                        operator_username LIKE ? ESCAPE '\\'
                        OR business_module LIKE ? ESCAPE '\\'
                        OR business_type LIKE ? ESCAPE '\\'
                        OR business_id LIKE ? ESCAPE '\\'
                        OR business_name LIKE ? ESCAPE '\\'
                        OR request_uri LIKE ? ESCAPE '\\'
                     )
                    """);
            for (int i = 0; i < 6; i++) {
                args.add(like);
            }
        }
        return where.toString();
    }

    private RowMapper<OperationLog> operationLogRowMapper() {
        return (resultSet, rowNum) -> {
            OperationLog log = new OperationLog();
            log.setId(resultSet.getLong("id"));
            log.setOperatorUsername(resultSet.getString("operator_username"));
            log.setOperatorUserType(resultSet.getString("operator_user_type"));
            log.setOperationType(resultSet.getString("operation_type"));
            log.setBusinessModule(resultSet.getString("business_module"));
            log.setBusinessType(resultSet.getString("business_type"));
            log.setBusinessId(resultSet.getString("business_id"));
            log.setBusinessName(resultSet.getString("business_name"));
            log.setBeforeData(resultSet.getString("before_data"));
            log.setAfterData(resultSet.getString("after_data"));
            log.setClientIp(resultSet.getString("client_ip"));
            log.setRequestUri(resultSet.getString("request_uri"));
            log.setRequestMethod(resultSet.getString("request_method"));
            log.setOperationAt(localDateTime(resultSet, "operation_at"));
            log.setCreatedAt(localDateTime(resultSet, "created_at"));
            return log;
        };
    }

    private java.time.LocalDateTime localDateTime(ResultSet resultSet, String column) throws SQLException {
        Timestamp timestamp = resultSet.getTimestamp(column);
        return timestamp == null ? null : timestamp.toLocalDateTime();
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
