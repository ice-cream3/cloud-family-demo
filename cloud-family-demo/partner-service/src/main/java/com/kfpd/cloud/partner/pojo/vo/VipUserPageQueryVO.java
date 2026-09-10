package com.kfpd.cloud.partner.pojo.vo;

public record VipUserPageQueryVO(
        Integer pageNum,
        Integer pageSize,
        String username,
        String displayName,
        String vipLevel,
        String status
) {
}
