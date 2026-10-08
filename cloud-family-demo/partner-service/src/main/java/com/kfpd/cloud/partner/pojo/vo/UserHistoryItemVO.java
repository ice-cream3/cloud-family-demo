package com.kfpd.cloud.partner.pojo.vo;

import java.time.LocalDateTime;

public record UserHistoryItemVO(
        Long id,
        String category,
        String title,
        String description,
        String iconTone,
        LocalDateTime occurredAt
) {
}
