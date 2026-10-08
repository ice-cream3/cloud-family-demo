package com.kfpd.cloud.manager.service.impl;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.kfpd.cloud.common.config.datasource.MultiDataSourceNames;
import com.kfpd.cloud.manager.dao.cloud.UserFeatureAdminDao;
import com.kfpd.cloud.manager.pojo.vo.PageQueryVO;
import com.kfpd.cloud.manager.pojo.vo.PageVO;
import com.kfpd.cloud.manager.service.OperationLogService;
import com.kfpd.cloud.manager.service.UserFeatureAdminService;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserFeatureAdminServiceImpl implements UserFeatureAdminService {

    private static final String MODULE_PARTNER = "PARTNER";

    private final UserFeatureAdminDao userFeatureAdminDao;
    private final OperationLogService operationLogService;

    public UserFeatureAdminServiceImpl(UserFeatureAdminDao userFeatureAdminDao, OperationLogService operationLogService) {
        this.userFeatureAdminDao = userFeatureAdminDao;
        this.operationLogService = operationLogService;
    }

    @Override
    public PageVO<Map<String, Object>> findPage(String feature, PageQueryVO query) {
        int pageNum = SystemManagementSupport.pageNum(query);
        int pageSize = SystemManagementSupport.pageSize(query);
        int offset = (pageNum - 1) * pageSize;
        String keyword = SystemManagementSupport.keyword(query);
        String status = SystemManagementSupport.status(query);
        String category = query == null ? null : trimToNull(query.businessType());
        long total;
        List<Map<String, Object>> records;
        switch (feature) {
            case "notifications" -> {
                total = userFeatureAdminDao.countNotifications(keyword);
                records = userFeatureAdminDao.findNotificationPage(keyword, offset, pageSize);
            }
            case "documents" -> {
                total = userFeatureAdminDao.countDocuments(keyword);
                records = userFeatureAdminDao.findDocumentPage(keyword, offset, pageSize);
            }
            case "versions" -> {
                total = userFeatureAdminDao.countVersions(keyword);
                records = userFeatureAdminDao.findVersionPage(keyword, offset, pageSize);
            }
            case "memberships" -> {
                total = userFeatureAdminDao.countMemberships(keyword, status);
                records = userFeatureAdminDao.findMembershipPage(keyword, status, offset, pageSize);
            }
            case "histories" -> {
                total = userFeatureAdminDao.countHistories(keyword, category);
                records = userFeatureAdminDao.findHistoryPage(keyword, category, offset, pageSize);
            }
            case "favorites" -> {
                total = userFeatureAdminDao.countFavorites(keyword);
                records = userFeatureAdminDao.findFavoritePage(keyword, offset, pageSize);
            }
            case "trip-users" -> {
                total = userFeatureAdminDao.countTripOwners(keyword);
                records = userFeatureAdminDao.findTripOwnerPage(keyword, offset, pageSize);
            }
            case "trip-publishes" -> {
                total = userFeatureAdminDao.countTrips(keyword, status, false);
                records = userFeatureAdminDao.findTripPage(keyword, status, false, offset, pageSize);
            }
            case "trip-reviews" -> {
                total = userFeatureAdminDao.countTrips(keyword, status, true);
                records = userFeatureAdminDao.findTripPage(keyword, status, true, offset, pageSize);
            }
            default -> throw SystemManagementSupport.notFound("Feature not found");
        }
        return new PageVO<>(total, pageNum, pageSize, records);
    }

    @Override
    @Transactional(transactionManager = MultiDataSourceNames.FA_CLOUD_TRANSACTION_MANAGER)
    public Map<String, Object> create(String feature, Map<String, Object> record) {
        Map<String, Object> normalized = normalize(record);
        int affected = switch (feature) {
            case "notifications" -> userFeatureAdminDao.insertNotification(normalized);
            case "documents" -> userFeatureAdminDao.insertDocument(normalized);
            case "versions" -> userFeatureAdminDao.insertVersion(normalized);
            case "memberships" -> userFeatureAdminDao.insertMembership(normalized);
            case "histories" -> userFeatureAdminDao.insertHistory(normalized);
            case "favorites" -> userFeatureAdminDao.insertFavorite(normalized);
            case "trip-publishes", "trip-reviews" -> userFeatureAdminDao.insertTrip(normalized);
            case "trip-users" -> throw SystemManagementSupport.badRequest("trip user summary cannot be created");
            default -> throw SystemManagementSupport.notFound("Feature not found");
        };
        if (affected == 0) {
            throw SystemManagementSupport.badRequest("create failed");
        }
        operationLogService.recordCreate(MODULE_PARTNER, businessType(feature), numberId(normalized), businessName(normalized), normalized);
        return normalized;
    }

    @Override
    @Transactional(transactionManager = MultiDataSourceNames.FA_CLOUD_TRANSACTION_MANAGER)
    public Map<String, Object> update(String feature, Long id, Map<String, Object> record) {
        Map<String, Object> normalized = normalize(record);
        int affected = switch (feature) {
            case "notifications" -> userFeatureAdminDao.updateNotification(id, normalized);
            case "documents" -> userFeatureAdminDao.updateDocument(id, normalized);
            case "versions" -> userFeatureAdminDao.updateVersion(id, normalized);
            case "memberships" -> userFeatureAdminDao.updateMembership(id, normalized);
            case "histories" -> userFeatureAdminDao.updateHistory(id, normalized);
            case "favorites" -> userFeatureAdminDao.updateFavorite(id, normalized);
            case "trip-publishes", "trip-reviews" -> userFeatureAdminDao.updateTrip(id, normalized);
            case "trip-users" -> throw SystemManagementSupport.badRequest("trip user summary cannot be updated");
            default -> throw SystemManagementSupport.notFound("Feature not found");
        };
        if (affected == 0) {
            throw SystemManagementSupport.notFound("Feature record not found");
        }
        normalized.put("id", id);
        operationLogService.recordUpdate(MODULE_PARTNER, businessType(feature), id, businessName(normalized), "UPDATED", normalized);
        return normalized;
    }

    @Override
    @Transactional(transactionManager = MultiDataSourceNames.FA_CLOUD_TRANSACTION_MANAGER)
    public void delete(String feature, Long id) {
        int affected = switch (feature) {
            case "notifications" -> userFeatureAdminDao.deleteNotification(id);
            case "documents" -> userFeatureAdminDao.deleteDocument(id);
            case "versions" -> userFeatureAdminDao.deleteVersion(id);
            case "memberships" -> userFeatureAdminDao.deleteMembership(id);
            case "histories" -> userFeatureAdminDao.deleteHistory(id);
            case "favorites" -> userFeatureAdminDao.deleteFavorite(id);
            case "trip-publishes", "trip-reviews" -> userFeatureAdminDao.deleteTrip(id);
            case "trip-users" -> throw SystemManagementSupport.badRequest("trip user summary cannot be deleted");
            default -> throw SystemManagementSupport.notFound("Feature not found");
        };
        if (affected == 0) {
            throw SystemManagementSupport.notFound("Feature record not found");
        }
        operationLogService.recordDelete(MODULE_PARTNER, businessType(feature), id, String.valueOf(id), Map.of("id", id));
    }

    private Map<String, Object> normalize(Map<String, Object> record) {
        if (record == null) {
            throw SystemManagementSupport.badRequest("request body is required");
        }
        return record;
    }

    private String businessType(String feature) {
        return "USER_" + feature.toUpperCase();
    }

    private String businessName(Map<String, Object> record) {
        return Optional.ofNullable(record.get("title"))
                .or(() -> Optional.ofNullable(record.get("username")))
                .or(() -> Optional.ofNullable(record.get("ownerUsername")))
                .or(() -> Optional.ofNullable(record.get("documentType")))
                .or(() -> Optional.ofNullable(record.get("versionName")))
                .map(String::valueOf)
                .orElse("-");
    }

    private Long numberId(Map<String, Object> record) {
        Object id = record.get("id");
        if (id instanceof Number number) {
            return number.longValue();
        }
        return null;
    }

    private String trimToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
