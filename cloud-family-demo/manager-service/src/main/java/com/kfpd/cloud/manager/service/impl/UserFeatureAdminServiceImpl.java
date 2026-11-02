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
            case "commerce-shops" -> { total=userFeatureAdminDao.countCommerceShops(keyword,status); records=userFeatureAdminDao.findCommerceShops(keyword,status,offset,pageSize); }
            case "commerce-products" -> { total=userFeatureAdminDao.countCommerceProducts(keyword,status); records=userFeatureAdminDao.findCommerceProducts(keyword,status,offset,pageSize); }
            case "showcase" -> {
                total = userFeatureAdminDao.countShowcase(keyword, status);
                records = userFeatureAdminDao.findShowcasePage(keyword, status, offset, pageSize);
            }
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
    public Map<String, Object> membershipDetail(Long id) {
        Map<String, Object> detail = userFeatureAdminDao.findMembershipDetail(id);
        if (detail == null) {
            throw SystemManagementSupport.notFound("Membership not found");
        }
        return detail;
    }

    @Override
    @Transactional(transactionManager = MultiDataSourceNames.FA_CLOUD_TRANSACTION_MANAGER)
    public Map<String, Object> create(String feature, Map<String, Object> record) {
        Map<String, Object> normalized = feature.startsWith("commerce-") ? normalizeCommerce(feature,record) : "showcase".equals(feature) ? normalizeShowcase(record) : normalize(record);
        int affected = switch (feature) {
            case "commerce-shops" -> userFeatureAdminDao.insertCommerceShop(normalized);
            case "commerce-products" -> userFeatureAdminDao.insertCommerceProduct(normalized);
            case "showcase" -> userFeatureAdminDao.insertShowcase(normalized);
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
        Map<String, Object> normalized = feature.startsWith("commerce-") ? normalizeCommerce(feature,record) : "showcase".equals(feature) ? normalizeShowcase(record) : normalize(record);
        int affected = switch (feature) {
            case "commerce-shops" -> userFeatureAdminDao.updateCommerceShop(id, normalized);
            case "commerce-products" -> userFeatureAdminDao.updateCommerceProduct(id, normalized);
            case "showcase" -> userFeatureAdminDao.updateShowcase(id, normalized);
            case "notifications" -> userFeatureAdminDao.updateNotification(id, normalized);
            case "documents" -> userFeatureAdminDao.updateDocument(id, normalized);
            case "versions" -> userFeatureAdminDao.updateVersion(id, normalized);
            case "memberships" -> userFeatureAdminDao.updateMembership(id, normalized);
            case "histories" -> throw SystemManagementSupport.badRequest("历史记录不允许修改");
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
            case "commerce-shops" -> {
                if (userFeatureAdminDao.countCommerceShopProducts(id)>0) throw SystemManagementSupport.badRequest("请先删除店铺中的全部商品，或将店铺停用");
                yield userFeatureAdminDao.deleteCommerceShop(id);
            }
            case "commerce-products" -> userFeatureAdminDao.deleteCommerceProduct(id);
            case "showcase" -> userFeatureAdminDao.deleteShowcase(id);
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

    private Map<String,Object> normalizeCommerce(String feature,Map<String,Object> record) {
        normalize(record);
        if ("commerce-shops".equals(feature)) {
            var result=com.kfpd.cloud.common.showcase.CommerceRules.shop(record);
            result.put("ownerUsername",com.kfpd.cloud.common.showcase.CommerceRules.text(record,"ownerUsername",64,true));
            result.put("status",com.kfpd.cloud.common.showcase.CommerceRules.state(record.get("status"),List.of("ACTIVE","BLOCKED"),"ACTIVE"));
            result.put("sortOrder",com.kfpd.cloud.common.showcase.CommerceRules.integer(record.get("sortOrder"),0,Integer.MAX_VALUE));
            return result;
        }
        var result=com.kfpd.cloud.common.showcase.CommerceRules.product(record);
        result.put("shopId",com.kfpd.cloud.common.showcase.CommerceRules.id(record.get("shopId")));
        result.put("moderationStatus",com.kfpd.cloud.common.showcase.CommerceRules.state(record.get("moderationStatus"),List.of("ACTIVE","BLOCKED"),"ACTIVE"));
        boolean blocked="BLOCKED".equals(result.get("moderationStatus"));
        String reason=com.kfpd.cloud.common.showcase.CommerceRules.text(record,"moderationReason",500,blocked);
        result.put("moderationReason",blocked?reason:null);
        if (blocked) result.put("status","OFFLINE");
        return result;
    }

    private Map<String, Object> normalizeShowcase(Map<String, Object> record) {
        normalize(record);
        Map<String, Object> normalized = new java.util.HashMap<>();
        String[] keys = {"title", "category", "summary", "content"};
        int[] limits = {100, 32, 300, 100000};
        for (int i = 0; i < keys.length; i++) {
            Object raw = record.get(keys[i]);
            if (!(raw instanceof String value) || value.isBlank() || value.trim().length() > limits[i]) {
                throw SystemManagementSupport.badRequest("标题、分类、摘要和正文不能为空且不能超长");
            }
            normalized.put(keys[i], value.trim());
        }
        String status = record.get("status") instanceof String value ? value : "DRAFT";
        if (!List.of("DRAFT", "PUBLISHED", "OFFLINE").contains(status)) {
            throw SystemManagementSupport.badRequest("精品状态无效");
        }
        normalized.put("status", status);
        String cover = record.get("coverUrl") instanceof String value ? value.trim() : "";
        if (!cover.isEmpty()) {
            try {
                java.net.URI uri = java.net.URI.create(cover);
                boolean relative = cover.startsWith("/") && !cover.startsWith("//") && uri.getHost() == null;
                boolean remote = ("https".equalsIgnoreCase(uri.getScheme()) || "http".equalsIgnoreCase(uri.getScheme())) && uri.getHost() != null;
                if (cover.length() > 1024 || (!relative && !remote)) throw new IllegalArgumentException();
            } catch (IllegalArgumentException ex) {
                throw SystemManagementSupport.badRequest("封面需填写 http(s) 图片地址或站内绝对路径");
            }
        }
        normalized.put("coverUrl", cover.isEmpty() ? null : cover);
        try {
            normalized.put("sortOrder", Integer.parseInt(String.valueOf(record.getOrDefault("sortOrder", 0))));
        } catch (NumberFormatException ex) {
            throw SystemManagementSupport.badRequest("排序必须是整数");
        }
        return normalized;
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
