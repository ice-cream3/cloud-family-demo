package com.kfpd.cloud.manager.pojo.vo;

public record VipUserPageQueryVO(
        Integer pageNum,
        Integer pageSize,
        String username,
        String displayName,
        String vipLevel,
        String status
) {
}
