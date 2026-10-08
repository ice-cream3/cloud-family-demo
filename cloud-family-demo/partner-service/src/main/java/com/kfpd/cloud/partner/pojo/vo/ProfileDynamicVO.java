package com.kfpd.cloud.partner.pojo.vo;

public record ProfileDynamicVO(
        long favoriteCount,
        long historyCount,
        long fileCount,
        long couponCount,
        String serviceStatus,
        String refreshedAt
) {
}
