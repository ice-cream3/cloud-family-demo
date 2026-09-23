package com.kfpd.cloud.manager.pojo.vo;

public record LoginTrendPointVO(
        String date,
        long managerLoginCount,
        long partnerLoginCount
) {
}
