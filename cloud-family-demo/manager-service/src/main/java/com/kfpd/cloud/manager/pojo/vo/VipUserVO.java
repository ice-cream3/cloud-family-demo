package com.kfpd.cloud.manager.pojo.vo;

import java.time.LocalDateTime;

public record VipUserVO(
        Long id,
        String username,
        String displayName,
        String email,
        String phone,
        String vipLevel,
        String status,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
