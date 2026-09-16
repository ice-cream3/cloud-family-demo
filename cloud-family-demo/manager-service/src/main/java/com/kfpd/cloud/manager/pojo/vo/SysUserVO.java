package com.kfpd.cloud.manager.pojo.vo;

import java.time.LocalDateTime;

public record SysUserVO(
        Long id,
        String username,
        String displayName,
        String email,
        String status,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
